package com.sschr15.scanlation

import com.kotlindiscord.kord.extensions.DiscordRelayedException
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.events.EventContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.mongodb.client.model.Filters.eq
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.message.MessageBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlin.math.ceil

class ScanlationExtension : Extension() {
	override val name = "scanlations"

	private val projects = db.getCollection<Project>(Project.COLLECTION_NAME)

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "project"
			description = "Manage a project"
			project()
		}

		ephemeralSlashCommand {
			name = "chapter"
			description = "Manage a chapter"
			chapter()
		}

		ephemeralSlashCommand {
			name = "role"
			description = "Manage project and chapter roles"

			ephemeralSubCommand(::RoleModifyArguments) {
				name = "add"
				description = "Add a role to a project or chapter"

				action {
					val project = projects.find(eq(arguments.project)).firstOrNull()
						?: throw DiscordRelayedException("No project with that name found.")

					if (arguments.chapter != null) {
						val chapter = project.chapters.find { it.identifier == arguments.chapter }
							?: throw DiscordRelayedException("No chapter with that identifier found.")

						val task = arguments.role
						if (task in chapter.completedTasks) {
							throw DiscordRelayedException("That task is already completed.")
						}

						val incompleteTasks = chapter.incompleteTasks.toMutableMap()
						val assignedUsers = incompleteTasks.getOrDefault(task, emptySet()).toMutableSet()
						val user = arguments.user.id

						if (!assignedUsers.add(user)) {
							throw DiscordRelayedException("That user is already assigned to that task.")
						}

						incompleteTasks[task] = assignedUsers

						val newChapter = chapter.copy(incompleteTasks = incompleteTasks)
						val newChapters = project.chapters.toMutableList().apply {
							set(indexOf(chapter), newChapter)
						}

						val replacement = project.copy(chapters = newChapters)
						projects.replaceOne(eq(project.internalName), replacement)

						respond {
							content = "Role added to chapter ${chapter.identifier}."
						}
					} else {
						val task = arguments.role
						if (task in project.primaryMembers) {
							throw DiscordRelayedException("That task is already completed.")
						}

						val assignedUsers = project.primaryMembers.getOrDefault(task, emptySet()).toMutableSet()
						val user = arguments.user.id

						if (!assignedUsers.add(user)) {
							throw DiscordRelayedException("That user is already assigned to that task.")
						}

						val newPrimaryMembers = project.primaryMembers.toMutableMap().apply {
							set(task, assignedUsers)
						}

						val replacement = project.copy(primaryMembers = newPrimaryMembers)
						projects.replaceOne(eq(project.internalName), replacement)

						respond {
							content = "Role added to project."
							projectEmbed(replacement)
						}
					}
				}
			}

			ephemeralSubCommand(::RoleModifyArguments) {
				name = "remove"
				description = "Remove a role from a project or chapter"

				action {
					val project = projects.find(eq(arguments.project)).firstOrNull()
						?: throw DiscordRelayedException("No project with that name found.")

					if (arguments.chapter != null) {
						val chapter = project.chapters.find { it.identifier == arguments.chapter }
							?: throw DiscordRelayedException("No chapter with that identifier found.")

						val task = arguments.role
						if (task in chapter.completedTasks) {
							throw DiscordRelayedException("That task is already completed.")
						}

						val incompleteTasks = chapter.incompleteTasks.toMutableMap()
						val assignedUsers = incompleteTasks.getOrDefault(task, emptySet()).toMutableSet()
						val user = arguments.user.id

						if (!assignedUsers.remove(user)) {
							throw DiscordRelayedException("That user is not assigned to that task.")
						}

						incompleteTasks[task] = assignedUsers

						val newChapter = chapter.copy(incompleteTasks = incompleteTasks)
						val newChapters = project.chapters.toMutableList().apply {
							set(indexOf(chapter), newChapter)
						}

						val replacement = project.copy(chapters = newChapters)
						projects.replaceOne(eq(project.internalName), replacement)

						respond {
							content = "Role removed from chapter ${chapter.identifier}."
						}
					} else {
						val task = arguments.role
						if (task in project.primaryMembers) {
							throw DiscordRelayedException("That task is already completed.")
						}

						val assignedUsers = project.primaryMembers.getOrDefault(task, emptySet()).toMutableSet()
						val user = arguments.user.id

						if (!assignedUsers.remove(user)) {
							throw DiscordRelayedException("That user is not assigned to that task.")
						}

						val newPrimaryMembers = project.primaryMembers.toMutableMap().apply {
							set(task, assignedUsers)
						}

						val replacement = project.copy(primaryMembers = newPrimaryMembers)
						projects.replaceOne(eq(project.internalName), replacement)

						respond {
							content = "Role removed from project."
							projectEmbed(replacement)
						}
					}
				}
			}

			ephemeralSubCommand(::UnsetRolesArguments) {
				name = "view-unset"
				description = "View unassigned roles for a project or chapter"

				action {
					val targetedProjects = if (arguments.project != null) {
						listOf(projects.find(eq(arguments.project)).firstOrNull()
							?: throw DiscordRelayedException("No project with that name found."))
					} else {
						projects.find().toList()
					}

					if (arguments.project == null && arguments.chapter != null) {
						throw DiscordRelayedException("You must specify a project to view unassigned roles for a chapter.")
					}

					respond {
						content = "Unassigned roles"
						embed {
							targetedProjects.forEach { project ->
								title = project.name

								if (arguments.chapter != null) {
									val chapter = project.chapters.find { it.identifier == arguments.chapter }
										?: throw DiscordRelayedException("No chapter with that identifier found.")

									description = "Chapter ${chapter.identifier}"
									field {
										name = "Unassigned Roles"
										value = chapter.incompleteTasks.entries.joinToString("\n") { (k, v) ->
											val asString = v.joinToString(", ") { "<@$it>" }
											"- ${k.readableName}: $asString"
										}
									}
								} else {
									field {
										name = "Unassigned Roles"
										value = project.primaryMembers.entries.joinToString("\n") { (k, v) ->
											val asString = v.joinToString(", ") { "<@$it>" }
											"- ${k.readableName}: $asString"
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private suspend fun EphemeralSlashCommand<*, *>.project() {
		ephemeralSubCommand(::ProjectCreateArguments) {
			name = "create"
			description = "Create a new project"

			action {
				val internalName = arguments.internalName ?: arguments.name.lowercase().replace(" ", "-")

				if (projects.find(eq(internalName)).count() != 0) {
					throw DiscordRelayedException("A project with that internal name ($internalName) already exists.")
				}

				val project = Project(
					arguments.name,
					arguments.longName,
					internalName,
					arguments.lead.id,
					mutableMapOf(),
					mutableListOf()
				)

				projects.insertOne(project)

				respond {
					content = "Project created."
					projectEmbed(project)
				}
			}
		}

		ephemeralSubCommand(::ProjectModifyArguments) {
			name = "edit"
			description = "Edit a project"

			action {
				val project = projects.find(eq(arguments.project)).firstOrNull()
					?: throw DiscordRelayedException("No project with that name found.")

				if (arguments.name != null) project.name = arguments.name!!
				if (arguments.longName != null) project.longName = arguments.longName
				if (arguments.lead != null) project.lead = arguments.lead!!.id
				if (arguments.folderUrl != null) project.folderUrl = arguments.folderUrl
				if (arguments.rawsUrl != null) project.rawsUrl = arguments.rawsUrl
				if (arguments.mangadexUrl != null) project.mangadexUrl = arguments.mangadexUrl
				if (arguments.state != null) project.state = arguments.state!!
				if (arguments.channel != null) project.staffChannel = arguments.channel!!.id

				projects.replaceOne(eq(project.internalName), project)

				respond {
					content = "Project updated."
					projectEmbed(project)
				}
			}
		}

		publicSubCommand(::GenerateVolunteerMessageArguments) {
			name = "generate-volunteer-message"
			description = "Generate a message to ask for volunteers"

			action {
				val project = projects.find(eq(arguments.project)).firstOrNull()
					?: throw DiscordRelayedException("No project with that name found.")

				val message = "Use one of the buttons here to volunteer if help is needed for ${project.name}"

				respond {
					content = message

					volunteerButtons(project.internalName)
				}
			}
		}

		ephemeralSubCommand(::RequestHelpArguments) {
			name = "request-help"
			description = "Request help with a task in a chapter"

			action {
				val project = projects.find(eq(arguments.project)).firstOrNull()
					?: throw DiscordRelayedException("No project with that name found.")

				val task = arguments.task
				val message = arguments.message
					?: "${task.readableName} help requested for ${project.name}!"

				val volunteers = project.volunteerMembers[task]?.takeUnless { it.isEmpty() }
					?: throw DiscordRelayedException("No volunteers are listed for that task in that project.")

				channel.createMessage {
					content = buildString {
						append(message)
						append(" (Automatic ping to ${volunteers.joinToString(", ") { "<@$it>" }})")
					}
				}

				respond {
					content = "Help requested."
				}
			}
		}

		event<ButtonInteractionCreateEvent> {
			check {
				failIfNot { event.interaction.componentId.startsWith("volunteer:") }
				failIf { event.interaction.componentId.split(':').size != 3 }
			}

			action {
				volunteer()
			}
		}
	}

	private fun MessageBuilder.volunteerButtons(projectInternal: String) {
		val rows = Task.entries.let { it.chunked(ceil(it.size / 2.0).toInt()) }
		rows.forEach { tasks ->
			actionRow {
				tasks.forEach { task ->
					interactionButton(ButtonStyle.Primary, "volunteer:${task.name}:$projectInternal") {
						label = task.readableName
					}
				}
			}
		}
	}

	private suspend fun EventContext<ButtonInteractionCreateEvent>.volunteer() = with(event.interaction.deferEphemeralResponse()) {
		val task = Task.valueOf(event.interaction.componentId.split(":")[1])
		val projectInternal = event.interaction.componentId.split(":")[2]

		val project = projects.find(eq(projectInternal)).firstOrNull()
			?: throw DiscordRelayedException("Unable to find a project with internal name $projectInternal. This is likely a bug.")

		val volunteers = project.volunteerMembers.getOrDefault(task, setOf()).toMutableSet()

		if (event.interaction.user.id in volunteers) {
			alreadyVolunteering(task, project, projectInternal)
			return@with
		}

		volunteers.add(event.interaction.user.id)

		val newVolunteers = project.volunteerMembers.toMutableMap().apply {
			set(task, volunteers)
		}

		val replacement = project.copy(volunteerMembers = newVolunteers)
		projects.replaceOne(eq(project.internalName), replacement)

		respond {
			content = "Volunteer status added for ${task.readableName}."
		}
	}

	private suspend fun DeferredMessageInteractionResponseBehavior.alreadyVolunteering(
		task: Task,
		project: Project,
		projectInternal: String
	) {
		respond {
			content = "You are already volunteering for ${task.readableName} in ${project.name}. " +
				"Would you like to remove your volunteer status?"

			components {
				ephemeralButton {
					label = "Remove"
					id = "remove-volunteer:${task.name}:$projectInternal"
					style = ButtonStyle.Danger

					@Suppress("NAME_SHADOWING")
					action {
						// Refetch the project in case it's been modified since the button was pressed
						val project = projects.find(eq(projectInternal)).firstOrNull()
							?: throw DiscordRelayedException("Unable to find a project with internal name $projectInternal. This is likely a bug.")

						val volunteers = project.volunteerMembers.getOrDefault(task, setOf()).toMutableSet()

						volunteers.remove(user.id)

						val newVolunteers = project.volunteerMembers.toMutableMap().apply {
							set(task, volunteers)
						}

						val replacement = project.copy(volunteerMembers = newVolunteers)
						projects.replaceOne(eq(project.internalName), replacement)

						respond {
							content = "Volunteer status removed."
						}
					}
				}
			}
		}
	}

	private suspend fun EphemeralSlashCommand<*, *>.chapter() {
		ephemeralSubCommand(::ChapterCreateArguments) {
			name = "create"
			description = "Create a new chapter"

			action {
				val project = projects.find(eq(arguments.project)).firstOrNull()
					?: throw DiscordRelayedException("No project with that name found.")

				val identifier = arguments.identifier
				if (project.chapters.any { it.identifier == identifier }) {
					throw DiscordRelayedException("A chapter with that identifier already exists.")
				}

				val chapter = Chapter(
					identifier,
					arguments.title,
					mutableMapOf(),
					project.primaryMembers
				)

				val newChapters = project.chapters.toMutableList().apply {
					add(chapter)
				}

				project.chapters = newChapters
				projects.replaceOne(eq(project.internalName), project)

				respond {
					content = "Chapter created."
					chapterEmbed(chapter)
				}
			}
		}

		ephemeralSubCommand(::ChapterProgressArguments) {
			name = "finish"
			description = "Finish a task in a chapter"

			action {
				val project = projects.find(eq(arguments.project)).firstOrNull()
					?: throw DiscordRelayedException("No project with that name found.")

				val chapter = project.chapters.find { it.identifier == arguments.identifier }
					?: throw DiscordRelayedException("No chapter with that identifier found.")

				val task = arguments.task
				if (task in chapter.completedTasks) {
					throw DiscordRelayedException("That task is already completed.")
				}

				val incompleteTasks = chapter.incompleteTasks.toMutableMap()
				val completedTasks = chapter.completedTasks.toMutableMap()

				val completedUsers = arguments.user?.id?.let { setOf(it) } ?: incompleteTasks.getOrDefault(task, emptySet())

				incompleteTasks.remove(task)
				completedTasks[task] = completedUsers

				val newChapter = chapter.copy(
					completedTasks = completedTasks,
					incompleteTasks = incompleteTasks
				)

				val newChapters = project.chapters.toMutableList().apply {
					set(indexOf(chapter), newChapter)
				}

				project.chapters = newChapters
				projects.replaceOne(eq(project.internalName), project)

				if (project.staffChannel != null) {
					val channel = this@ephemeralSubCommand.kord.getChannelOf<MessageChannel>(project.staffChannel!!)
						?: throw DiscordRelayedException("Unable to find the staff channel for this project. This is likely a bug.")

					channel.createMessage {
						content = "Task ${task.readableName} completed for chapter ${chapter.identifier}."
					}
				}

				respond {
					content = "Task completed."
					chapterEmbed(newChapter)
				}
			}
		}

		ephemeralSubCommand(::ChapterStatusArguments) {
			name = "status"
			description = "Get the status of a chapter"

			action {
				val project = projects.find(eq(arguments.project)).firstOrNull()
					?: throw DiscordRelayedException("No project with that name found.")

				if (arguments.identifier != null) {
					val chapter = project.chapters.find { it.identifier == arguments.identifier }
						?: throw DiscordRelayedException("No chapter with that identifier found.")

					respond {
						content = "Chapter status"
						chapterEmbed(chapter)
					}
					return@action
				}

				val filteredList = project.chapters.filter { it.releasedUrl == null }

				if (filteredList.isEmpty()) throw DiscordRelayedException("No unreleased chapters found.")

				editingPaginator {
//					chunkedPages = 5
					timeoutSeconds = 300 // 5 minutes

					filteredList.forEach {
						page {
							chapterEmbed(it)
						}
					}
				}.send()
			}
		}

		ephemeralSubCommand(::ChapterPublishArguments) {
			name = "publish"
			description = "Mark a chapter as released"

			action {
				val project = projects.find(eq(arguments.project)).firstOrNull()
					?: throw DiscordRelayedException("No project with that name found.")

				val chapter = project.chapters.find { it.identifier == arguments.identifier }
					?: throw DiscordRelayedException("No chapter with that identifier found.")

				val all = chapter.completedTasks + chapter.incompleteTasks

				val replacement = chapter.copy(
					releasedUrl = arguments.url ?: "",
					completedTasks = all.takeIf { arguments.markAllTasks } ?: chapter.completedTasks,
					incompleteTasks = chapter.incompleteTasks.takeUnless { arguments.markAllTasks } ?: emptyMap()
				)

				val newChapters = project.chapters.toMutableList().apply {
					set(indexOf(chapter), replacement)
				}

				project.chapters = newChapters
				projects.replaceOne(eq(project.internalName), project)

				respond {
					content = "Chapter published."
					chapterEmbed(replacement)
				}
			}
		}
	}
}
