## Overview

A program for **reviewing chess opening lines**.

This tool is designed for **repetition, not discovery**.  
Opening lines must be prepared in advance by the user — either found manually or created with the help of an engine — and then added to the program.

The current goal is to keep the product **minimal and focused**, with possible extra features added later.

## Goals

- **Fully offline operation**
    - The program must work without an internet connection.
    - Internet access is still not always available, and the app should remain fully usable offline.

- **Opening line repetition**
    - The same line played from **White's side** and **Black's side** is treated as **two different lines**.
    - Even if the resulting FEN positions are identical, the training purpose is different:
        - when studying as White, Black may play bad moves that White must punish
        - when studying as Black, Black should only play correct moves
    - Because of this, matching by FEN alone is not enough. An additional side/training context flag is required in the database.

- **Edit a line during training**
    - Rename a line with a human-readable title
    - Change line weight / priority
    - Delete a line
    - Shorten or extend a variation
    - Branch off from any move to create a new line

- **Find all lines containing a selected position**
    - Move number should be ignored
    - Side to move must be respected
    - This helps when many variations already exist and some of them need to be deactivated or removed after a repertoire change

- **Build a training plan based on line weight / priority**
    - Example:
        - Line 1 → priority 1
        - Line 2 → priority 3
        - Line 3 → priority 2
        - Line 4 → inactive
    - Training plan:
        - Line 1 repeated 1 time
        - Line 2 repeated 3 times
        - Line 3 repeated 2 times
    - After all required repetitions are completed, the plan is considered finished

- **Training plan templates**
    - Create reusable templates
    - For example:
        - for a specific opening
        - from a specific position
        - for a specific training goal

- **Statistics collection**
    - Track training performance for each line
    - The main metric for now is **mistake frequency during repetition**
    - In the future, statistics may be used to generate training plans automatically

- **Position metadata**
    - Add custom metadata to any position
    - For example:
        - comments
        - evaluation
        - notes
        - any other useful information