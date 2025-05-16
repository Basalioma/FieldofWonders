package com.example.fieldofwonders.data

data class Player(
   val name: String,
   val isBot: Boolean = false,
   var score: Int = 0,
   val id: Int
)