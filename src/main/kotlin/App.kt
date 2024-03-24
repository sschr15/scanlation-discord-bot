package com.sschr15.scanlation

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.adapters.mongodb.mongoDB
import com.kotlindiscord.kord.extensions.utils.env

private val TOKEN = env("TOKEN")   // Get the bot' token from the env vars or a .env file

suspend fun main() {
	val bot = ExtensibleBot(TOKEN) {
		mongoDB()

		extensions {
			add(::ScanlationExtension)
		}
	}

	bot.start()
}
