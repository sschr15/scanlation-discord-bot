package com.sschr15.scanlation

import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.embed

typealias Multimap<K, V> = Map<K, Set<V>>
typealias MutableMultimap<K, V> = MutableMap<K, MutableSet<V>>

fun MessageBuilder.projectEmbed(project: Project) {
	embed {
		title = project.name

		if (project.longName != null) {
			description = "**Full Name:** ${project.longName}"
		}

		field {
			name = "Current State"
			value = project.state.readableName
			inline = true
		}

		field {
			name = "Internal Name"
			value = project.internalName
			inline = true
		}

		field {
			name = "Lead"
			value = "<@${project.lead}>"
			inline = true
		}

		field {
			name = "Primary Members"

			value = project.primaryMembers.entries.joinToString("\n") { (k, v) ->
				val asString = v.joinToString(", ") { "<@$it>" }
				"- ${k.readableName}: ${if (v.isEmpty()) "None assigned" else asString}"
			}
			inline = true
		}

		field {
			name = "URLs"
			value = buildString {
				if (project.folderUrl != null) {
					appendLine("[Drive Folder](${project.folderUrl})")
				} else {
					appendLine("Drive Folder (unset)")
				}

				if (project.rawsUrl != null) {
					appendLine("[Raws](${project.rawsUrl})")
				} else {
					appendLine("Raws (unset)")
				}

				if (project.mangadexUrl != null) {
					appendLine("[MangaDex](${project.mangadexUrl})")
				} else {
					appendLine("MangaDex (unset)")
				}
			}
			inline = true
		}
	}
}

fun MessageBuilder.chapterEmbed(chapter: Chapter) {
	embed {
		chapterEmbed(chapter)
	}
}

fun EmbedBuilder.chapterEmbed(chapter: Chapter) {
	title = "Chapter ${chapter.identifier}"

	if (chapter.title != null) {
		description = chapter.title
	}

	field {
		name = "Completed Tasks"
		value = chapter.completedTasks.entries.joinToString("\n") { (k, v) ->
			val asString = v.joinToString(", ") { "<@$it>" }
			"- ${k.readableName}: ${if (v.isEmpty()) "None assigned" else asString}"
		}.takeIf { it.isNotBlank() } ?: "(None listed)"
		inline = true
	}

	field {
		name = "Incomplete Tasks"
		value = chapter.incompleteTasks.entries.joinToString("\n") { (k, v) ->
			val asString = v.joinToString(", ") { "<@$it>" }
			"- ${k.readableName}: ${if (v.isEmpty()) "None assigned" else asString}"
		}.takeIf { it.isNotBlank() } ?: "(None listed)"
		inline = true
	}

	if (!chapter.releasedUrl.isNullOrEmpty()) {
		field {
			name = "Release URL"
			value = "[Link](${chapter.releasedUrl})"
			inline = true
		}
	}
}
