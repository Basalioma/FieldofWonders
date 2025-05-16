package com.example.fieldofwonders.logic

import com.example.fieldofwonders.data.DrumSector
import kotlin.random.Random

class DrumManager {
   private val sectors = listOf(
      DrumSector.Points(350), DrumSector.Points(350),
      DrumSector.Points(400), DrumSector.Points(400),
      DrumSector.Points(450), DrumSector.Points(450),
      DrumSector.Points(500), DrumSector.Points(500),
      DrumSector.Points(550), DrumSector.Points(550),
      DrumSector.Points(600), DrumSector.Points(600),
      DrumSector.Points(650), DrumSector.Points(650),
      DrumSector.Points(700), DrumSector.Points(700),
      DrumSector.Points(750), DrumSector.Points(750),
      DrumSector.Points(800), DrumSector.Points(800),
      DrumSector.Points(850), DrumSector.Points(850),
      DrumSector.Points(900), DrumSector.Points(900),
      DrumSector.Points(950), DrumSector.Points(950),
      DrumSector.Points(1000), DrumSector.Points(1000),
      DrumSector.Double, DrumSector.Double,
      DrumSector.Zero, DrumSector.Zero,
      DrumSector.Plus, DrumSector.Plus,
      DrumSector.Bankrupt, DrumSector.Bankrupt
   )

   fun spin(): DrumSector {
      return sectors[Random.nextInt(sectors.size)]
   }

   // Добавляем публичный метод для доступа к секторам
   fun getAllSectors(): List<DrumSector> = sectors
}