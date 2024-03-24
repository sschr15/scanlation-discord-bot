package com.sschr15.scanlation

import com.kotlindiscord.kord.extensions.adapters.mongodb.kordExCodecRegistry
import com.kotlindiscord.kord.extensions.utils.env
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.kotlinx.KotlinSerializerCodecProvider

private val client = MongoClient.create(env("ADAPTER_MONGODB_URI")) // Use the same as kordex

val db = client.getDatabase("scanlation-bot")
	.withCodecRegistry(CodecRegistries.fromRegistries(
		kordExCodecRegistry,
		CodecRegistries.fromProviders(KotlinSerializerCodecProvider()),
		MongoClientSettings.getDefaultCodecRegistry()
	))
