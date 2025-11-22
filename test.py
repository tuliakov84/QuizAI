import requests
import json
import os

url = "http://localhost:11434/api/chat"

topic = input("Введите тему: ")
n = int(input("Введите количество вопросов: "))
quiz_id = input("Введите ID пакета вопросов: ")
filename = "package" + quiz_id

payload = {
    "model": "qwen2.5",
    "format": "json",
    "stream": False,
    "messages": [
        {
            "role": "user",
            "content": f"""
You are a quiz generator AI.

Your task:
Generate {n} multiple-choice questions on the topic "{topic}".
All output MUST be fully in Russian.
Each question must have exactly 4 answer choices and must indicate the correct one.

Below is the JSON template for a single question:

{{
    "number": <k + 1>,
    "question": "<question text in Russian>",
    "available_answers":
    [
        {{
            "index": 1, 
            "answer": "<answer1>" 
        }},
        {{
            "index": 2,
            "answer": "<answer2>"
        }},
        {{
            "index": 3,
            "answer": "<answer3>"
        }},
        {{
            "index": 4,
            "answer": "<answer4>" 
        }}
    ],
    "right_ans_index": <correct answer index>
}}

FINAL OUTPUT FORMAT (strict):
[
    {{
        "questions":
            [
            ... {n} question objects following the template above ...
            ]
    }}
]

Rules:
1. The final output must be ONLY valid JSON.
2. No explanations, comments, markdown, or any text outside JSON.
3. The top-level JSON must be an array (outer square brackets).
4. The array must contain exactly one object, which has the key "questions".
5. The "questions" value must be an array of exactly {n} question objects.
6. The field "number" must go from 1 to {n}.
7. All question texts and answers must be strictly in Russian.
8. Questions must be unique and not repeat phrasing/answers.
9. ALL indentation, spaces, line breaks, and JSON structure MUST strictly follow the provided template.
   Formatting is absolutely fixed:

   - Each level of nesting must use exactly two spaces for indentation.
   - Every opening curly brace and opening square bracket MUST appear on a new line.
   - Every closing curly brace and closing square bracket MUST also appear on a new line.
   - Opening and closing braces/brackets may NEVER appear on the same line as other content.
   - Arrays and objects must always begin with an opening bracket/brace on one line and end with a closing bracket/brace on its own separate line.
   - Every field must start on a new line.
   - A line break must follow every comma.
   - No extra spaces, no missing spaces, no empty lines, no comments, no markdown, and no text outside of the JSON are allowed.

   Any deviation from the structural or visual formatting of the template is strictly forbidden.
"""
        }
    ]
}


response = requests.post(url, json=payload, stream=True)

answer = ""

for line in response.iter_lines():
    if not line:
        continue
    try:
        item = json.loads(line.decode("utf-8"))
    except:
        continue

    if "message" in item and "content" in item["message"]:
        answer += item["message"]["content"]

try:
    parsed = json.loads(answer)
except Exception as e:
    print("Error: unknown JSON result structure")
    raise e

downloadsFolder = os.path.join(os.path.expanduser("~"), "Downloads")
file_path = os.path.join(downloadsFolder, f"{filename}.json")

with open(file_path, "w", encoding="utf-8") as f:
    json.dump(parsed, f, ensure_ascii=False, indent=2)

print(f"done! file: {file_path}")

removeTestFileAnswer = input(f"Удалить тестовый файл? y/n: ")
if removeTestFileAnswer == "y":
    os.remove(file_path)