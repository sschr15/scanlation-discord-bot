package com.sschr15.scanlation

import com.kotlindiscord.kord.extensions.commands.application.slash.converters.ChoiceEnum
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Task(override val readableName: String, vararg dependencies: Task) : ChoiceEnum {
	Raws("Raws"),
	CleanerRedrawer("CL/RD", Raws),
	Translator("TL", Raws),
	Proofreader("PR", Translator),
	Typesetter("TS", CleanerRedrawer, Proofreader),
	QualityChecker("QC", Typesetter),

	;

	val dependencies = dependencies.toSet()

	val dependents: Set<Task> by lazy {
		entries.filter { this in it.dependencies }.toSet()
	}
}

@Serializable
enum class ProjectState(override val readableName: String) : ChoiceEnum {
	Active("Active"),
	Hiatus("On Hiatus"),
	Completed("Completed"),
	Dropped("Dropped"),
}

@Serializable
data class Project(
	var name: String,
	var longName: String?,
	@SerialName("_id")
	val internalName: String,
	var lead: Snowflake,
	var primaryMembers: Multimap<Task, Snowflake>,
	var chapters: List<Chapter>,
	var staffChannel: Snowflake? = null,
//	var autoPingSetting: Boolean? = null,
	var folderUrl: String? = null,
	var rawsUrl: String? = null,
	var mangadexUrl: String? = null,
	var volunteerMembers: Multimap<Task, Snowflake> = emptyMap(),
	var state: ProjectState = ProjectState.Active,
) {
	companion object {
		const val COLLECTION_NAME = "projects"
	}
}

@Serializable
data class Chapter(
	val identifier: String,
	val title: String?,
	val completedTasks: Multimap<Task, Snowflake>,
	val incompleteTasks: Multimap<Task, Snowflake>,
	val releasedUrl: String? = null,
) : Comparable<Chapter> {
	override fun compareTo(other: Chapter): Int {
		val myParts = identifier.split('.').map { it.toInt() }
		val otherParts = other.identifier.split('.').map { it.toInt() }

		if (myParts.first() != otherParts.first()) return myParts.first().compareTo(otherParts.first())

		return when(myParts.size to otherParts.size) {
			1 to 2 -> -1
			2 to 1 -> 1
			2 to 2 -> myParts[1].compareTo(otherParts[1])
			else -> 0
		}
	}
}
