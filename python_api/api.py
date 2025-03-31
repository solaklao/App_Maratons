from flask import Flask, request, jsonify
import openai
import requests
import time
import json
import logging
import re
import os
from dotenv import load_dotenv

# Загрузка переменных из .env файла
load_dotenv()


app = Flask(__name__)

# Set your OpenAI API key
openai.api_key = os.getenv("OPENAI_API_KEY")

# Настроить логирование
logging.basicConfig(level=logging.INFO)
app.logger = logging.getLogger(__name__)

# Available nationalities for reference
NATIONALITIES = {
    'AU': 'Australia', 'BR': 'Brazil', 'CA': 'Canada', 'CH': 'Switzerland',
    'DE': 'Germany', 'DK': 'Denmark', 'ES': 'Spain', 'FI': 'Finland',
    'FR': 'France', 'GB': 'United Kingdom', 'IE': 'Ireland', 'IN': 'India',
    'IR': 'Iran', 'MX': 'Mexico', 'NL': 'Netherlands', 'NO': 'Norway',
    'NZ': 'New Zealand', 'RS': 'Serbia', 'TR': 'Turkey', 'UA': 'Ukraine',
    'US': 'United States'
}

def get_name_list():
    """
    Retrieves a list of Latvian names from the API and processes them to identify likely gender.
    Returns a list of dictionaries containing names and their likely gender.
    """
    url = "https://tools.csb.gov.lv/names/api?action=articles"
    response = requests.get(url)
    if response.status_code == 200:
        data = response.json()
        names = []
        for article in data['data']:
            # Process comma-separated names
            article_names = article['article_names'].split(',')
            for name in article_names:
                name = name.strip()
                if name:
                    # In Latvian, female names typically end in 'a' or 'e',
                    # but not if they end in 'is'
                    is_female = name.endswith(('a', 'e')) and not name.endswith('is')
                    names.append({
                        'name': name,
                        'likely_female': is_female
                    })

            # Extract names from story text (marked with <b> tags)
            story_names = re.findall(r'<b>([^<]+)<\/b>', article['story_lv'])
            for name in story_names:
                name = name.strip()
                if name and not any(d['name'] == name for d in names):
                    is_female = name.endswith(('a', 'e')) and not name.endswith('is')
                    names.append({
                        'name': name,
                        'likely_female': is_female
                    })

        return names
    else:
        return []

def get_random_user(gender, nationality):
    url = f"https://randomuser.me/api/?gender={gender}&nat={nationality}"
    response = requests.get(url)
    if response.status_code == 200:
        return response.json()['results'][0]
    return None

def get_cities_by_letter(letter=''):
    url = "https://gateway.spark-dev.team/cabinet/api/v2/cities"
    try:
        response = requests.get(url)
        if response.status_code == 200:
            response_data = response.json()
            cities = response_data.get('data', [])
            
            # Create a mapping between Russian and Latin characters
            russian_to_latin = {
                'А': 'A', 'Б': 'B', 'В': 'V', 'Г': 'G', 'Д': 'D', 'Е': 'E', 'Ё': 'YO', 
                'Ж': 'ZH', 'З': 'Z', 'И': 'I', 'Й': 'Y', 'К': 'K', 'Л': 'L', 'М': 'M', 
                'Н': 'N', 'О': 'O', 'П': 'P', 'Р': 'R', 'С': 'S', 'Т': 'T', 'У': 'U', 
                'Ф': 'F', 'Х': 'KH', 'Ц': 'TS', 'Ч': 'CH', 'Ш': 'SH', 'Щ': 'SCH', 'Ъ': '', 
                'Ы': 'Y', 'Ь': '', 'Э': 'E', 'Ю': 'YU', 'Я': 'YA'
            }
            # Add lowercase versions
            for k, v in list(russian_to_latin.items()):
                russian_to_latin[k.lower()] = v.lower()
                
            # Create a reverse mapping (for single letters only)
            latin_to_russian = {}
            for rus, lat in russian_to_latin.items():
                if len(lat) == 1:  # Only single-letter mappings for reverse lookup
                    latin_to_russian[lat] = rus
            
            # Function to convert Latin letter to Russian
            def latin_to_russian_letter(letter):
                return latin_to_russian.get(letter, letter)
            
            # If letter provided, convert Latin to Russian if needed
            search_letter = ''
            if letter:
                # Check if this is a Latin letter that needs conversion
                search_letter = latin_to_russian_letter(letter)
            
            # Filter results
            if search_letter:
                # Filter by the Russian letter (which might be the original letter if not found in map)
                filtered_cities = []
                for city in cities:
                    if isinstance(city, dict) and 'name' in city:
                        city_name = city['name']
                        # Check if the city name starts with our search letter
                        if city_name and city_name.lower().startswith(search_letter.lower()):
                            # Just collect the original city name - we'll let the AI assistant do the translation
                            filtered_cities.append(city_name)
                
                # Return just the city names - the AI assistant will translate them
                return filtered_cities
            else:
                # Return all cities
                return [city['name'] for city in cities if isinstance(city, dict) and 'name' in city]
        else:
            return f"Error retrieving data. Status: {response.status_code}"
    except Exception as e:
        return f"An error occurred: {str(e)}"

tools = [
    {
        "type": "function",
        "function": {
            "name": "get_name_list",
            "description": "Returns a list of Latvian names with basic gender information",
            "parameters": {
                "type": "object",
                "properties": {},
                "required": []
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_random_user",
            "description": "Returns random user data based on specified gender and nationality",
            "parameters": {
                "type": "object",
                "properties": {
                    "gender": {
                        "type": "string",
                        "enum": ["male", "female"],
                        "description": "Gender of the user"
                    },
                    "nationality": {
                        "type": "string",
                        "enum": list(NATIONALITIES.keys()),
                        "description": "Two-letter country code for nationality"
                    }
                },
                "required": ["gender", "nationality"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_cities_by_letter",
            "description": "Gets a list of cities starting with a specific letter or combination of letters",
            "parameters": {
                "type": "object",
                "properties": {
                    "letter": {
                        "type": "string",
                        "description": "Letter or combination of letters to search for cities"
                    }
                },
                "required": ["letter"]
            }
        }
    }
]

# Create four specialized assistants
system_assistant = openai.beta.assistants.create(
    name="System Command Assistant",
    instructions="""You are specialized in handling system commands and general queries.

Your main responsibilities:
1. Recognize and handle system commands in any form:
   - Help/commands/what can you do/etc → Return "SYSTEM:HELP"
   - Exit/quit/close/etc → Return "SYSTEM:EXIT"
   - Nationalities/countries/etc → Return "SYSTEM:NATIONALITIES"

2. Provide clear system responses and handle general queries
3. Respond only in English
4. If unsure, direct to the appropriate specialized assistant""",
    model="gpt-4o-mini",
    tools=tools
)

latvian_names_assistant = openai.beta.assistants.create(
    name="Latvian Names Assistant",
    instructions="""You are specialized in working with Latvian names.

Your main responsibilities:
1. Handle all queries related to Latvian names
2. Apply Latvian name rules:
   - Female names typically end in -a or -e
   - Male names typically end in -is
3. Use the get_name_list function to retrieve names
4. Filter and present names based on gender and other criteria
5. List only matching names without additional text
6. Include all relevant name variations
7. Respond only in English""",
    model="gpt-4o-mini",
    tools=tools
)

random_user_assistant = openai.beta.assistants.create(
    name="Random User Assistant",
    instructions="""You are specialized in generating random user profiles.

Your main responsibilities:
1. Handle all queries related to random user generation
2. Recognize country names and match them to country codes:
   - Handle variations: USA/America → Use "US"
   - Handle misspellings and variations
   - If country isn't in list, return "ERROR: [Country] not available"
3. Return user data in JSON format including:
   - name, gender, location, email, phone, date of birth, picture
4. Use the get_random_user function with appropriate parameters
5. Format responses without markdown symbols
6. Respond only in English""",
    model="gpt-4o-mini",
    tools=tools
)

city_search_assistant = openai.beta.assistants.create(
    name="City Search Assistant",
    instructions="""You are specialized in city search operations.

Your main responsibilities:
1. Handle all queries related to city searches
2. Use the get_cities_by_letter function to find cities
3. IMPORTANT: Understand both Latin and Cyrillic characters - if user enters a Russian letter (like "Ф"), convert it to Latin equivalent ("F") and vice versa
4. IMPORTANT: You will receive results from the API in Russian. You should translate these city names to English properly (not just transliterate). For example:
   - "Китай" should be translated as "China" (not "Kitay")
   - "Москва" should be translated as "Moscow"
   - For city names, provide appropriate English translations if they exist
   - For city names that don't have standard English translations, provide a proper transliteration
   - If a city name includes a country or region in parentheses, translate that part properly as well
5. Return city lists in a clear, readable format WITHOUT any extra text, explanation, or introduction
6. Respond only in English
7. When a user asks for cities starting with a letter, always use the get_cities_by_letter function with that letter as parameter, even if it's a Russian/Cyrillic letter""",
    model="gpt-4o-mini",
    tools=tools
)

# Dictionary to map query types to assistants
assistants = {
    'system': system_assistant,
    'names': latvian_names_assistant,
    'users': random_user_assistant,
    'cities': city_search_assistant
}

@app.route('/api/chat', methods=['POST'])
def chat():
    try:
        data = request.json
        user_input = data.get('message', '').strip()
        
        if not user_input:
            return jsonify({"error": "No message provided"}), 400
        
        # Определяем, какого ассистента использовать на основе запроса
        assistant_type = 'system'  # по умолчанию системный ассистент
        query_lower = user_input.lower()

        if any(word in query_lower for word in ['name', 'names', 'latvian']):
            assistant_type = 'names'
        elif any(word in query_lower for word in ['user', 'person']):
            assistant_type = 'users'
        elif any(word in query_lower for word in ['city', 'cities']):
            assistant_type = 'cities'

        selected_assistant = assistants[assistant_type]
        
        # Создаем новый поток для каждого запроса
        thread = openai.beta.threads.create()
        app.logger.info(f"Created new thread {thread.id} for message: {user_input[:30]}...")

        # Создаем сообщение в потоке
        message = openai.beta.threads.messages.create(
            thread_id=thread.id,
            role="user",
            content=user_input
        )

        # Запускаем ассистента
        run = openai.beta.threads.runs.create(
            thread_id=thread.id,
            assistant_id=selected_assistant.id
        )
        app.logger.info(f"Started run {run.id} with assistant {assistant_type}")

        # Ожидаем завершения с таймаутом
        while run.status not in ["completed", "requires_action", "failed"]:
            time.sleep(1)
            run = openai.beta.threads.runs.retrieve(thread_id=thread.id, run_id=run.id)

        if run.status == "requires_action":
            tool_outputs = []
            for tool_call in run.required_action.submit_tool_outputs.tool_calls:
                if tool_call.function.name == "get_name_list":
                    output = get_name_list()
                    names_str = str([{'n': item['name'], 'f': item['likely_female']} for item in output])
                    tool_outputs.append({
                        "tool_call_id": tool_call.id,
                        "output": names_str
                    })
                elif tool_call.function.name == "get_random_user":
                    args = json.loads(tool_call.function.arguments)
                    output = get_random_user(args.get('gender'), args.get('nationality'))
                    tool_outputs.append({
                        "tool_call_id": tool_call.id,
                        "output": json.dumps(output)
                    })
                elif tool_call.function.name == "get_cities_by_letter":
                    args = json.loads(tool_call.function.arguments)
                    cities = get_cities_by_letter(args.get('letter', ''))
                    tool_outputs.append({
                        "tool_call_id": tool_call.id,
                        "output": json.dumps(cities, ensure_ascii=False)
                    })

            run = openai.beta.threads.runs.submit_tool_outputs(
                thread_id=thread.id,
                run_id=run.id,
                tool_outputs=tool_outputs
            )

            while run.status not in ["completed", "failed"]:
                time.sleep(1)
                run = openai.beta.threads.runs.retrieve(thread_id=thread.id, run_id=run.id)

        if run.status == "completed":
            messages = openai.beta.threads.messages.list(thread_id=thread.id)
            response = messages.data[0].content[0].text.value

            # Обрабатываем системные команды
            if response.startswith("SYSTEM:"):
                command = response.split(":")[1]
                if command == "HELP":
                    return jsonify({
                        "system_command": True,
                        "command": "help",
                        "response": """Available commands:
1. For Latvian names:
   - Names starting with letter [letter]
2. For random users:
   - Woman/man from [country]
   - Nationalities - list of available countries
3. For city search:
   - Cities starting with letter [letter]
  """
                    })
                elif command == "NATIONALITIES":
                    nationalities_str = "\n".join([f"{code}: {name}" for code, name in NATIONALITIES.items()])
                    return jsonify({
                        "system_command": True,
                        "command": "nationalities",
                        "response": f"Available nationalities:\n{nationalities_str}"
                    })
                elif command == "EXIT":
                    return jsonify({
                        "system_command": True,
                        "command": "exit",
                        "response": "Session ended."
                    })

            # Обрабатываем другие ответы
            try:
                # Пробуем разобрать как JSON (для данных о случайном пользователе)
                user_data = json.loads(response)
                if isinstance(user_data, dict) and 'name' in user_data:
                    formatted_response = f"""Name: {user_data['name']['title']} {user_data['name']['first']} {user_data['name']['last']}
Gender: {user_data['gender']}
Location: {user_data['location']['city']}, {user_data['location']['state']}, {user_data['location']['country']}
Email: {user_data['email']}
Phone: {user_data['phone']}
Cell: {user_data['cell']}
Date of Birth: {user_data['dob']['date'][:10]} (Age: {user_data['dob']['age']})
Picture: {user_data['picture']['large']}"""
                    return jsonify({"response": formatted_response})
                else:
                    # Это может быть список городов или имен
                    return jsonify({"response": response})
            except:
                # Если не JSON, возвращаем как обычный текст
                return jsonify({"response": response})
        elif run.status == "failed":
            error_msg = "Request processing failed"
            if hasattr(run, 'last_error'):
                error_msg += f": {run.last_error}"
            app.logger.error(error_msg)
            return jsonify({"error": error_msg}), 500
        else:
            # Если статус неизвестный или запрос был отменен
            return jsonify({"error": f"Unexpected run status: {run.status}"}), 500

    except Exception as e:
        app.logger.error(f"Error in chat endpoint: {str(e)}")
        return jsonify({"error": f"An error occurred: {str(e)}"}), 500

@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({"status": "healthy"})

def main():
    print("Hello! I can help you with:")
    print("1. Finding Latvian names")
    print("2. Generating random users")
    print("3. Searching cities by letter")
    print("\nType 'exit' or 'quit' to end the session\n")

    # Создаем поток перед входом в цикл
    thread = openai.beta.threads.create()

    while True:
        print()
        user_input = input("Enter your query: ").strip()
        print()

        if user_input.lower() in ['exit', 'quit']:
            print("Goodbye!")
            break

        # Определяем, какого ассистента использовать на основе запроса
        assistant_type = 'system'  # по умолчанию системный ассистент
        query_lower = user_input.lower()

        if any(word in query_lower for word in ['name', 'names', 'latvian']):
            assistant_type = 'names'
        elif any(word in query_lower for word in ['user', 'person']):
            assistant_type = 'users'
        elif any(word in query_lower for word in ['city', 'cities']):
            assistant_type = 'cities'

        selected_assistant = assistants[assistant_type]

        # Создаем сообщение в потоке
        message = openai.beta.threads.messages.create(
            thread_id=thread.id,
            role="user",
            content=user_input
        )

        # Запускаем ассистента
        run = openai.beta.threads.runs.create(
            thread_id=thread.id,
            assistant_id=selected_assistant.id
        )

        while run.status not in ["completed", "requires_action", "failed"]:
            time.sleep(1)
            run = openai.beta.threads.runs.retrieve(thread_id=thread.id, run_id=run.id)

        if run.status == "requires_action":
            tool_outputs = []
            for tool_call in run.required_action.submit_tool_outputs.tool_calls:
                if tool_call.function.name == "get_name_list":
                    output = get_name_list()
                    names_str = str([{'n': item['name'], 'f': item['likely_female']} for item in output])
                    tool_outputs.append({
                        "tool_call_id": tool_call.id,
                        "output": names_str
                    })
                elif tool_call.function.name == "get_random_user":
                    args = json.loads(tool_call.function.arguments)
                    output = get_random_user(args.get('gender'), args.get('nationality'))
                    tool_outputs.append({
                        "tool_call_id": tool_call.id,
                        "output": json.dumps(output)
                    })
                elif tool_call.function.name == "get_cities_by_letter":
                    args = json.loads(tool_call.function.arguments)
                    cities = get_cities_by_letter(args.get('letter', ''))
                    tool_outputs.append({
                        "tool_call_id": tool_call.id,
                        "output": json.dumps(cities, ensure_ascii=False)
                    })

            run = openai.beta.threads.runs.submit_tool_outputs(
                thread_id=thread.id,
                run_id=run.id,
                tool_outputs=tool_outputs
            )

            while run.status not in ["completed", "failed"]:
                time.sleep(1)
                run = openai.beta.threads.runs.retrieve(thread_id=thread.id, run_id=run.id)

        if run.status == "completed":
            messages = openai.beta.threads.messages.list(thread_id=thread.id)
            response = messages.data[0].content[0].text.value

            # Обрабатываем системные команды
            if response.startswith("SYSTEM:"):
                command = response.split(":")[1]
                if command == "EXIT":
                    print("Session ended.")
                    break
                elif command == "HELP":
                    print("""Available commands:
1. For Latvian names:
   - Names starting with [letter]
2. For random users:
   - Woman/man from [country]
   - Nationalities - list of available countries
3. For city search:
   - Cities starting with ...
   - Find cities beginning with ...
4. exit/quit - exit the program""")
                elif command == "NATIONALITIES":
                    print("Available nationalities:")
                    for code, name in NATIONALITIES.items():
                        print(f"{code}: {name}")
                continue

            # Обработка ответов
            try:
                # Пробуем разобрать как JSON (для данных о случайном пользователе)
                user_data = json.loads(response)
                if isinstance(user_data, dict) and 'name' in user_data:
                    print(f"Name: {user_data['name']['title']} {user_data['name']['first']} {user_data['name']['last']}")
                    print(f"Gender: {user_data['gender']}")
                    print(f"Location: {user_data['location']['city']}, {user_data['location']['state']}, {user_data['location']['country']}")
                    print(f"Email: {user_data['email']}")
                    print(f"Phone: {user_data['phone']}")
                    print(f"Cell: {user_data['cell']}")
                    print(f"Date of Birth: {user_data['dob']['date'][:10]} (Age: {user_data['dob']['age']})")
                    print(f"Picture: {user_data['picture']['large']}")
                else:
                    # Если это список городов или имен
                    print(response)
            except:
                # Если не JSON, выводим как обычный текст
                print(response)

        print()

if __name__ == "__main__":
    # Можно выбрать между запуском Flask-приложения или интерактивного режима
    # main()  # Запуск интерактивного режима
    app.run(debug=True, host='0.0.0.0', port=5000)  # Запуск Flask-сервера