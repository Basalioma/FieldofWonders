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

class WordLoader(private val context: Context) {
    private val client = OkHttpClient()
    private val url = "https://raw.githubusercontent.com/Basalioma/A-list-of-words-for-Field-of-Wonders-/main/words.csv"
    private val prefs = context.getSharedPreferences("word_prefs", Context.MODE_PRIVATE)
    private val cacheFile = File(context.filesDir, "words.csv")

    suspend fun loadWords(): List<Word> {
        val localVersion = prefs.getInt("version", 0)
        val serverVersion = getServerVersion()

        return if (isInternetAvailable(context) && serverVersion > localVersion) {
            val csvString = fetchCsvFromServer()
            if (csvString != null) {
                saveCsvToFile(csvString, serverVersion)
                parseCsv(csvString)
            } else {
                loadWordsFromCache()
            }
        } else {
            loadWordsFromCache()
        }
    }

    private suspend fun getServerVersion(): Int = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val csvString = response.body?.string() ?: return@withContext 0
        val reader = CSVReaderBuilder(StringReader(csvString))
            .withCSVParser(com.opencsv.CSVParserBuilder().withSeparator('/').build())
            .build()
        val firstLine = reader.readNext()
        firstLine?.get(1)?.toIntOrNull() ?: 0
    }

    private suspend fun fetchCsvFromServer(): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseCsv(csvString: String): List<Word> {
        val reader = CSVReaderBuilder(StringReader(csvString))
            .withCSVParser(com.opencsv.CSVParserBuilder().withSeparator('/').build())
            .build()
        val lines = reader.readAll().drop(2)
        return lines.map { line -> Word(line[0], line[1]) }
    }

    private fun saveCsvToFile(csvString: String, version: Int) {
        cacheFile.writeText(csvString)
        prefs.edit {
            putInt("version", version)
        }
    }

    private fun loadWordsFromCache(): List<Word> {
        return if (cacheFile.exists()) {
            val csvString = cacheFile.readText()
            try {
                parseCsv(csvString)
            } catch (e: Exception) {
                loadWordsFromAssets()
            }
        } else {
            loadWordsFromAssets()
        }
    }

    private fun loadWordsFromAssets(): List<Word> {
        return try {
            val csvString = context.assets.open("words.csv").bufferedReader().use { it.readText() }
            parseCsv(csvString)
        } catch (e: Exception) {
            listOf(
                Word("КОТ", "Какое животное мурлыкает и ловит мышей?"),
                Word("ДОМ", "Где человек прячется от дождя?")
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