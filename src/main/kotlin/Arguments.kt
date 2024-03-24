package com.sschr15.scanlation

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.enumChoice
import com.kotlindiscord.kord.extensions.commands.converters.builders.ConverterBuilder
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.utils.suggestStringMap
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.TextChannelBehavior
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.toList
import kotlin.reflect.KMutableProperty0

class ProjectCreateArguments : Arguments() {
	val name by string {
		name = "name"
		description = "The name of the project"
	}

	val lead by user {
		name = "lead"
		description = "The lead of the project"
	}

	val longName by optionalString {
		name = "long-name"
		description = "The full name of the project. This is for series with very long names."
	}

	val internalName by optionalString {
		name = "internal-name"
		description = "The internal name of the project, for shortening. This cannot be changed later."
	}
}

class ProjectModifyArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by string {
		name = "project"
		description = "The project to modify"

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val name by optionalString {
		name = "name"
		description = "The new name of the project"
	}

	val longName by optionalString {
		name = "long-name"
		description = "The new full name of the project"
	}

	val lead by optionalUser {
		name = "lead"
		description = "The new lead of the project"
	}

	val folderUrl by optionalString {
		name = "folder-url"
		description = "The URL for the project's Drive folder"
	}

	val rawsUrl by optionalString {
		name = "raws-url"
		description = "The URL for the project's raw images"
	}

	val mangadexUrl by optionalString {
		name = "mangadex-url"
		description = "The URL for the project's MangaDex page"
	}

	val state by optionalEnum<ProjectState> {
		name = "state"
		description = "The current state of the project"
		typeName = "ProjectState"
	}

	val channel by optionalChannel {
		name = "channel"
		description = "The staff channel for the project"

		validate {
			failIf("Must provide a message channel in a guild") { value !is GuildMessageChannelBehavior? }
		}
	}
}

class ChapterCreateArguments : Arguments() {
	private val logger = KotlinLogging.logger {}
	private lateinit var projects: List<Project>

	init {
		logger.info { "ChapterCreateArguments initialized" }
	}

	val project by string {
		name = "project"
		description = "The project the chapter is in"

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val identifier by string {
		name = "identifier"
		description = "The identifier of the chapter"
	}

	val title by optionalString {
		name = "title"
		description = "The title of the chapter"
	}
}

class ChapterProgressArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by string {
		name = "project"
		description = "The project the chapter is in"

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val identifier by string {
		name = "identifier"
		description = "The identifier of the chapter"

		autocompleteChapter(::projects, ::projects.isInitialized)
	}

	val task by enumChoice<Task> {
		name = "task"
		description = "The task to complete"
		typeName = "task"
	}

	val user by optionalUser {
		name = "user"
		description = "The user who completed the task. If not provided, the current assignees will be used."
	}
}

class ChapterStatusArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by string {
		name = "project"
		description = "The project to get chapter status for"

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val identifier by optionalString {
		name = "identifier"
		description = "The identifier of a chapter. If not provided, all unreleased chapters will be shown."

		autocompleteChapter(::projects, ::projects.isInitialized)
	}
}

class ChapterPublishArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by string {
		name = "project"
		description = "The project the chapter is in"

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val identifier by string {
		name = "identifier"
		description = "The identifier of the chapter"

		autocompleteChapter(::projects, ::projects.isInitialized)
	}

	val url by optionalString {
		name = "url"
		description = "The URL of the released chapter"
	}

	val markAllTasks by defaultingBoolean {
		name = "mark-tasks"
		description = "If true, considers all tasks to be complete"
		defaultValue = false
	}
}

class RoleModifyArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by string {
		name = "project"
		description = "The project to interact with"

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val user by user {
		name = "user"
		description = "The user to operate on"
	}

	val role by enumChoice<Task> {
		name = "role"
		description = "The role to interact with"
		typeName = "task"
	}

	val chapter by optionalString {
		name = "chapter"
		description = "The chapter to interact with. If unspecified, the action will apply to the entire project."

		autocompleteChapter(::projects, ::projects.isInitialized)
	}
}

class UnsetRolesArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by optionalString {
		name = "project"
		description = "The project to view unassigned roles for. If unset, show all projects with unassigned roles."

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val chapter by optionalString {
		name = "chapter"
		description = "The chapter to view unassigned roles for. If unset, show all chapters with unassigned roles."

		autocompleteChapter(::projects, ::projects.isInitialized)
	}
}

class RequestHelpArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by string {
		name = "project"
		description = "The project to request help for"

		autocompleteProject(::projects, ::projects.isInitialized)
	}

	val task by enumChoice<Task> {
		name = "task"
		description = "The task to request help for"
		typeName = "task"
	}

	val message by optionalString {
		name = "message"
		description = "A message to include with the request. If not provided, a default message will be used."
	}
}

class GenerateVolunteerMessageArguments : Arguments() {
	private lateinit var projects: List<Project>

	val project by string {
		name = "project"
		description = "The project to generate a message for"

		autocompleteProject(::projects, ::projects.isInitialized)
	}
}

private fun ConverterBuilder<out String?>.autocompleteProject(projectsField: KMutableProperty0<List<Project>>, initialized: Boolean) {
	autoComplete {
		initIfNeeded(projectsField, initialized)

		suggestStringMap(projectsField.get().associate { it.name to it.internalName })
	}
}

private suspend fun initIfNeeded(projectsField: KMutableProperty0<List<Project>>, initialized: Boolean) {
	if (!initialized) {
		projectsField.set(
			db.getCollection<Project>(Project.COLLECTION_NAME)
				.find()
				.toList()
		)
	}
}

private fun ConverterBuilder<out String?>.autocompleteChapter(projectsField: KMutableProperty0<List<Project>>, initialized: Boolean) {
	autoComplete { event ->
		initIfNeeded(projectsField, initialized)

		val project = event.interaction.command.options["project"]?.value?.toString()

		if (project == null) {
			suggestStringMap(emptyMap())
			return@autoComplete
		}

		val projectObj = projectsField.get().find { it.internalName == project }

		if (projectObj == null) {
			suggestStringMap(emptyMap())
			return@autoComplete
		}

		suggestStringMap(projectObj.chapters.associate { (it.title ?: it.identifier) to it.identifier })
	}
}
