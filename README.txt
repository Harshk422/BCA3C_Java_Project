TIC TAC TOE - Java Mini Project (GUI)   |   Dharmit & Harsh
==========================================================
Requirement: JDK 11 or newer  (check with:  javac -version)

HOW TO RUN
  VS Code  : File > Open Folder > TicTacToeGUI, open src/Main.java, click "Run" above main().
  Windows  : double-click run.bat
  Mac/Linux: ./run.sh   (first time: chmod +x run.sh)
  Manual   : mkdir out
             javac -d out src/*.java
             java -cp out Main
The window opens FULL SCREEN (maximised) by itself.
All files are in the default package (no "package" line) and must stay together in src/.
Open this as its own folder - do not mix it into another project that uses "package tictactoe;".

MODULES -> FILES
  1 Game Board Module        Board.java                                   (Dharmit)
  2 Game Logic / Rules       GameLogic.java                               (Dharmit)
  3 Player Management        Player.java, HumanPlayer.java, PlayerManager.java (Dharmit)
  4 User Interface (GUI)     GameWindow.java, BoardPanel.java, RoundedButton.java,
                             Theme.java, ScoreCard.java, SoundManager.java (Harsh)
  5 Computer Player (AI)     ComputerPlayer.java                          (Harsh)
  6 Input Validation         InputValidator.java, InvalidInputException.java,
                             InvalidMoveException.java                    (Harsh)
  7 Save / Score History     ScoreHistory.java  -> creates scores.txt     (Shared)
  Extra: Achievements       Achievements.java (badges + streaks, saved in achievements.txt) (Shared)
  Main.java starts the program; GameWindow connects all modules.

FEATURES
  Core
   - Player vs Player, or vs Computer (Easy / Medium / Hard)
   - Clickable animated board with hover highlight and winning line
   - Wrong clicks and bad names show an error message
   - Every finished round is saved to scores.txt
  Extra features
   1. AUTO NEXT ROUND  - after a round ends, the next one starts by itself (3 s countdown)
   2. MATCH MODE       - first to 3 wins / first to 5 wins / endless; confetti + victory
                         tune when someone wins the match, then a new match starts itself
   3. HINT (3 per round) - the Hard AI (Minimax) highlights the best box for you
   4. UNDO (2 per round) - take back your last move (vs computer it removes the reply too)
   5. SOUND EFFECTS    - generated in code (no files needed), with a Sound ON/OFF button
   6. MEDIUM AI        - plays the best move ~60% of the time, otherwise random
   9. ACHIEVEMENTS     - 9 badges (First Blood, Hat-Trick, Unstoppable, Lightning, Flawless,
                         Computer Crusher, Survivor, Match Winner, Veteran);
                         menu > Achievements shows your progress
  10. WIN STREAKS      - win rounds in a row against the computer; the streak is shown
                         under the board and tracked in Achievements
  11. TURN TIMER       - "Timer: ON" gives 10 seconds per move (progress bar turns red);
                         when time is up a random move is played for you
  12. KEYBOARD SHORTCUTS - press 1-9 to play a box, U = undo, H = hint
   7. LEADERBOARD      - wins / draws / losses / win % for every player from scores.txt,
                         plus the history of recent rounds, with a Clear button

LOOK (NEON THEME)
  Dark navy-to-violet gradient, glowing cyan X / pink O / gold win line, glowing grid,
  neon pill buttons, animated title and X / Draws / O score cards (Theme.java holds the colours).

 
