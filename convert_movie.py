from pathlib import Path
path = Path('src/main/java/com/thestarwarscaller/core/Movie.java')
text = path.read_text(encoding='utf-8-sig')
path.write_text(text, encoding='ascii')
