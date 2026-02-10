import os
import requests
import datetime
import subprocess
from dbfread import DBF
# Для работы с RAR может потребоваться unrar.exe в PATH или библиотека rarfile с настройкой
# pip install rarfile dbfread requests
import rarfile

# Конфигурация
BASE_URL = "https://www.cbr.ru/vfs/credit/forms/"
DATA_DIR = "data"
# Пример: https://www.cbr.ru/vfs/credit/forms/101-20240101.rar

def download_form(form_number, date):
    """
    Скачивает архив с формой за определенную дату.
    date: datetime object
    """
    date_str = date.strftime("%Y%m%d")
    filename = f"{form_number}-{date_str}.rar"
    url = f"{BASE_URL}{filename}"
    local_path = os.path.join(DATA_DIR, filename)
    
    if not os.path.exists(DATA_DIR):
        os.makedirs(DATA_DIR)
        
    print(f"Downloading {url}...")
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    
    try:
        response = requests.get(url, headers=headers, stream=True)
        response.raise_for_status()
        
        with open(local_path, 'wb') as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
        print(f"Saved to {local_path}")
        return local_path
    except requests.exceptions.HTTPError as e:
        print(f"Error downloading {url}: {e}")
        return None

def _parse_dbf_file(extracted_path):
    print(f"Parsing {extracted_path}...")
    table = DBF(extracted_path, encoding='cp866') # ЦБ обычно использует cp866 или windows-1251
    records = []
    for i, record in enumerate(table):
        if i < 5: # Показываем первые 5 записей для теста
            print(record)
        records.append(record)
    print(f"Total records parsed: {len(records)}")
    return records

def extract_and_parse_dbf(archive_path):
    """
    Распаковывает RAR и читает DBF (упрощенно).
    Примечание: требуется unrar.
    """
    try:
        with rarfile.RarFile(archive_path) as rf:
            # Ищем файлы DBF внутри
            dbf_files = [f for f in rf.namelist() if f.lower().endswith('.dbf')]
            print(f"Found DBF files: {dbf_files}")
            
            # Для примера берем первый попавшийся (обычно там _B.dbf файлы баланса)
            if dbf_files:
                target_file = dbf_files[0]
                print(f"Extracting {target_file}...")
                rf.extract(target_file, path=DATA_DIR)
                
                extracted_path = os.path.join(DATA_DIR, target_file)
                return _parse_dbf_file(extracted_path)
                
    except rarfile.RarCannotExec as e:
        print(f"⚠️ Unrar not found ({e}). Attempting fallback to 7z...")
        try:
            # Fallback: используем 7zip (p7zip-full в Docker)
            # -y: yes to all, -o: output dir
            subprocess.run(["7z", "x", archive_path, f"-o{DATA_DIR}", "-y"], check=True, stdout=subprocess.DEVNULL)
            print("✅ 7z extraction successful.")
            
            # Ищем распакованный DBF (берем первый попавшийся в папке)
            for f in os.listdir(DATA_DIR):
                if f.lower().endswith('.dbf'):
                    return _parse_dbf_file(os.path.join(DATA_DIR, f))
        except Exception as e_7z:
            print(f"❌ 7z fallback failed: {e_7z}")
    except Exception as e:
        print(f"Error processing archive: {e}")

def process_records_to_chunks(records, form_type, date):
    """
    Преобразует сырые записи в текстовые документы для RAG.
    """
    chunks = []
    date_str = date.strftime("%d.%m.%Y")
    
    # Словарик для расшифровки счетов (упрощенно)
    account_names = {
        '20202': 'Касса кредитных организаций',
        '30102': 'Корреспондентские счета в Банке России',
        '40702': 'Коммерческие счета организаций',
        '40817': 'Счета физических лиц',
         # Можно подключить полный справочник счетов
    }

    # Группируем записи по регистрационному номеру банка (REGN)
    # Предполагаем, что в DBF есть поле REGN
    from collections import defaultdict
    bank_records = defaultdict(list)
    
    for record in records:
        # Ищем поле с номером банка (названия могут отличаться в разных DBF)
        regn = record.get('REGN') or record.get('NUM_SC') or 'UNKNOWN'
        bank_records[regn].append(record)

    for regn, items in bank_records.items():
        # Формируем сводный текст по банку
        # В реальности для RAG лучше делать чанки среднего размера (например, по группам счетов)
        
        # Пример: Чанк с основными балансовыми показателями
        summary_text = f"Отчетность по форме {form_type} за {date_str}. Банк (рег. номер {regn}).\n"
        
        # Считаем сумму активов (примерно)
        total_assets = 0.0
        
        details_text = []
        for item in items:
            # Ищем поля счета и суммы. В 101 форме обычно NUM_SC (счет) и ITOGO (сумма)
            account = str(item.get('NUM_SC', '')).strip()
            amount = float(item.get('ITOGO', 0) or 0)
            
            # Фильтрация мусора
            if not account or amount == 0:
                continue
                
            total_assets += amount
            
            account_name = account_names.get(account, 'Счет')
            details_text.append(f"Счет {account} ({account_name}): {amount:,.0f} руб.")
            
        summary_text += f"Общая сумма активов (расчетная): {total_assets:,.0f} руб.\n"
        summary_text += "Детализация значимых счетов:\n" + "\n".join(details_text[:20]) # Берем топ-20 для примера
        
        chunks.append({
            "content": summary_text,
            "metadata": {
                "source": f"cbr_form_{form_type}",
                "date": date_str,
                "bank_regn": str(regn),
                "type": "financial_report"
            }
        })
        
    return chunks

if __name__ == "__main__":
    # Тест: Данные за 1 января 2024
    test_date = datetime.date(2024, 1, 1)
    archive = download_form("101", test_date)
    
    if archive:
        records = extract_and_parse_dbf(archive)
        if records:
            chunks = process_records_to_chunks(records, "101", test_date)
            
            # Сохраняем результат в JSON для векторизации
            output_file = os.path.join(DATA_DIR, f"chunks_101_{test_date.strftime('%Y%m%d')}.json")
            import json
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(chunks, f, ensure_ascii=False, indent=2)
            print(f"Saved {len(chunks)} chunks to {output_file}")
