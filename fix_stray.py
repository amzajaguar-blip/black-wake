import sys

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if line.strip() == "}" and "fun startGame()" in "".join(lines[i+1:i+3]):
        # skip this stray brace
        continue
    new_lines.append(line)

with open("app/src/main/java/com/blackwake/game/GameViewModel.kt", "w") as f:
    f.writelines(new_lines)

