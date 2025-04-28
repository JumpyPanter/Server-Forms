# Server Forms

**Server Forms** is a Minecraft Fabric mod designed to enhance server interactions by allowing administrators to create and manage interactive forms for players. This mod provides a streamlined way to collect player input, feedback, or responses through customizable forms, making it ideal for surveys, event registrations, or any scenario requiring structured player interaction.

## What Does This Mod Do?
- **Interactive Forms**: Enables server administrators to create forms with multiple questions that players can answer directly in-game.
- **Custom Commands**: Each form can be assigned a unique command, allowing players to start forms easily by typing the corresponding command.
- **Response Management**: Player responses are stored in JSON files, making it easy to review and manage collected data.
- **Validation and Feedback**: Ensures that responses are validated, prevents duplicate submissions (if configured), and provides real-time feedback to players.

## Why Download This Mod?
- **Enhanced Server Interaction**: Improve communication and engagement with your players by collecting structured feedback or input.
- **Customizable and Flexible**: Create forms tailored to your server's needs, whether for events, surveys, or other purposes.
- **Ease of Use**: With auto-complete for commands and built-in validation, both administrators and players will find the mod intuitive and user-friendly.
- **Data Transparency**: Responses are logged and stored in a structured format, ensuring easy access and review.

## Critical Information
- **Base Configuration**: A default configuration file is included with the mod, containing example forms to help you get started. You can customize or add new forms by editing the configuration file.
- **File Storage**: Player responses are saved in the `mods/FormAnswers` directory as JSON files. Ensure this directory is accessible and properly secured.
- **Concurrency**: The mod does not currently handle concurrent file access. Avoid simultaneous modifications to the same form files to prevent data corruption.

## Commands
The following commands are available in Server Forms:

### Start a Form
- **Command**: `/[form_command]`
- **Description**: Starts the specified form for the player. Each form has a unique command defined in the configuration file.
- **Example**: `/feedback` (if the form's command is set to "feedback").

### Answer a Question
- **Command**: `/answer [response]`
- **Description**: Submits an answer to the current question in the active form session.
- **Example**: `/answer Yes`.

### View Form Responses
- **Command**: `/viewform [playername] [formname]`
- **Description**: Allows administrators to view a player's responses to a specific form. If no form name is provided, the latest form is displayed.
- **Example**: `/viewform Steve feedback`.

## Getting Started
1. Install the mod on your Minecraft server using the Fabric API.
2. A base configuration file will be generated on the first run, located in the `config` directory. This file contains example forms that you can modify or expand.
3. Use the commands listed above to interact with the forms in-game.
4. Player responses will be saved in the `mods/FormAnswers` directory for review.

---

Server Forms is designed to be lightweight, easy to use, and highly customizable, making it a valuable addition to any Minecraft server.

## Issues and Feedback
If you encounter bugs or want to suggest new features, please open an [Issue](../../issues) on the GitHub repository.


