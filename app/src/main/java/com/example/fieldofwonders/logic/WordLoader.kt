package com.example.fieldofwonders.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.edit
import com.example.fieldofwonders.data.Word
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.StringReader
import java.nio.charset.Charset

data class WordListData(val words: List<Word>, val version: Int)

class WordLoader(private val context: Context) {
    private val client = OkHttpClient()
    private val url = "https://raw.githubusercontent.com/Basalioma/A-list-of-words-for-Field-of-Wonders-/main/words.csv"
    private val prefs = context.getSharedPreferences("word_loader_prefs", Context.MODE_PRIVATE)
    private val cacheFile = File(context.filesDir, "words.csv")

    companion object {
        const val KEY_CACHED_VERSION = "cached_word_version"
    }

    suspend fun loadWords(): WordListData {
        val localVersion = prefs.getInt(KEY_CACHED_VERSION, 0)
        var serverVersion = -1 // Используем -1 как индикатор, что версия еще не получена
        var mustFetchFromServer = false

        if (isInternetAvailable(context)) {
            serverVersion = getServerVersion() // Пытаемся получить версию с сервера
            if (serverVersion > localVersion) {
                println("WordLoader: Server version ($serverVersion) is newer than local ($localVersion). Fetching...")
                mustFetchFromServer = true
            } else {
                println("WordLoader: Local version ($localVersion) is up-to-date or server unavailable (server version: $serverVersion). Using cache/assets.")
            }
        } else {
            println("WordLoader: No internet. Using cache/assets.")
        }

        return if (mustFetchFromServer) {
            val csvString = fetchCsvFromServer()
            if (csvString != null) {
                // Успешно загрузили с сервера
                saveCsvToFile(csvString, serverVersion) // Сохраняем файл и версию
                WordListData(parseCsv(csvString), serverVersion)
            } else {
                // Не смогли загрузить, используем кеш/assets
                println("WordLoader: Failed to fetch from server despite newer version. Falling back to cache/assets.")
                loadWordsFromCacheOrAssets()
            }
        } else {
            // Используем кеш/assets
            loadWordsFromCacheOrAssets()
        }
    }

    private suspend fun getServerVersion(): Int = withContext(Dispatchers.IO) {
        println("WordLoader: Fetching server version...")
        val request = Request.Builder().url(url).head().build() // Используем HEAD запрос, чтобы не качать весь файл
        var version = 0 // По умолчанию 0, если ошибка
        try {
            // HEAD запрос может не работать для raw github, делаем GET, но читаем мало
            val getRequest = Request.Builder().url(url).build()
            client.newCall(getRequest).execute().use { response -> // Используем use для автозакрытия
                if (response.isSuccessful) {
                    // Читаем только первые несколько строк, чтобы найти версию
                    response.body?.source()?.use { source ->
                        // Читаем не весь файл, а только начало, чтобы найти версию
                        // Это менее эффективно, чем HEAD, но более надежно для raw github
                        val firstFewLines = source.readUtf8Line() // Читаем первую строку (версия)
                        if (firstFewLines != null) {
                            version = parseVersionFromCsvLine(firstFewLines)
                            println("WordLoader: Parsed server version from first line: $version")
                        } else {
                            println("WordLoader: Could not read first line for version.")
                        }
                    }
                } else {
                    println("WordLoader: Failed to fetch server version, status code: ${response.code}")
                }
            }
        } catch (e: Exception) {
            println("WordLoader: Error fetching server version: ${e.message}")
        }
        println("WordLoader: Returning server version: $version")
        return@withContext version
    }

    private fun parseVersionFromCsvLine(line: String): Int {
        // Предполагаем формат "Version/1" или похожий
        return try {
            val parts = line.split('/', limit = 2)
            if (parts.size == 2) {
                parts[1].trim().toIntOrNull() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun fetchCsvFromServer(): String? = withContext(Dispatchers.IO) {
        println("WordLoader: Fetching full CSV from server...")
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response -> // Используем use
                if (response.isSuccessful) {
                    println("WordLoader: Successfully fetched CSV from server.")
                    response.body?.string() // Возвращаем строку
                } else {
                    println("WordLoader: Failed to fetch full CSV, status code: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            println("WordLoader: Error fetching full CSV: ${e.message}")
            null
        }
    }

    private fun parseCsv(csvString: String): List<Word> {
        // Можно оставить opencsv, если вы исправили CSV файл на GitHub
        return try {
            val reader = CSVReaderBuilder(StringReader(csvString))
                .withCSVParser(com.opencsv.CSVParserBuilder().withSeparator('/').build())
                .build()
            val lines = reader.readAll().drop(2) // Пропускаем версию и заголовки
            println("WordLoader (opencsv): Parsed ${lines.size} lines.")
            lines.mapNotNull { line -> // Используем mapNotNull для пропуска некорректных
                if (line.size >= 2 && line[0].isNotEmpty() && line[1].isNotEmpty()) {
                    Word(line[0].trim(), line[1].trim())
                } else {
                    println("WordLoader WARN (opencsv): Skipping invalid line: ${line.joinToString("|")}")
                    null
                }
            }
        } catch (e: Exception) {
            println("WordLoader ERROR (opencsv): Failed to parse CSV - ${e.message}. Falling back to empty list.")
            emptyList() // Возвращаем пустой список при ошибке парсинга
        }
        }

    private fun saveCsvToFile(csvString: String, version: Int) {
        try {
            cacheFile.writeText(csvString, Charsets.UTF_8)
            prefs.edit {
                putInt(KEY_CACHED_VERSION, version)
            }
            println("WordLoader: Saved CSV to cache. Version: $version")
        } catch (e: Exception) {
            println("WordLoader ERROR: Could not save CSV to cache: ${e.message}")
        }
    }

    private fun loadWordsFromCacheOrAssets(): WordListData {
        val cachedVersion = prefs.getInt(KEY_CACHED_VERSION, 0)
        println("WordLoader: Trying to load from cache (Version: $cachedVersion)...")
        if (cacheFile.exists()) {
            try {
                val csvString = cacheFile.readText(Charsets.UTF_8)
                // Пытаемся прочитать версию из самого кешированного файла, если он не пуст
                val versionFromCacheFile = csvString.lines().firstOrNull()?.let { parseVersionFromCsvLine(it) } ?: 0
                // Используем версию из файла, если она больше чем в prefs (на случай сбоя сохранения prefs)
                val finalVersion = maxOf(cachedVersion, versionFromCacheFile)
                println("WordLoader: Loaded from cache. Version from file: $versionFromCacheFile. Final version: $finalVersion")
                return WordListData(parseCsv(csvString), finalVersion)
            } catch (e: Exception) {
                println("WordLoader WARN: Failed to read or parse cache file (${e.message}). Falling back to assets.")
                // Ошибка чтения/парсинга кеша, падаем в assets
            }
        } else {
            println("WordLoader: Cache file does not exist.")
        }
        // Загрузка из assets как крайний случай
        println("WordLoader: Loading from assets...")
        return loadWordsFromAssets()
    }

    private fun loadWordsFromAssets(): WordListData {
        return try {
            val csvString = context.assets.open("words.csv").bufferedReader(Charsets.UTF_8).use { it.readText() }
            // Читаем версию из первой строки assets файла
            val version = csvString.lines().firstOrNull()?.let { parseVersionFromCsvLine(it) } ?: 0
            println("WordLoader: Loaded from assets. Version: $version")
            WordListData(parseCsv(csvString), version)
        } catch (e: Exception) {
            println("WordLoader ERROR: Failed to load from assets (${e.message}). Returning default words.")
            WordListData( // Возвращаем дефолтные слова с версией 0
                listOf(
                    Word("КОТ", "Какое животное мурлыкает и ловит мышей?"),
                    Word("ДОМ", "Где человек прячется от дождя?")
                ), 0
            )
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}