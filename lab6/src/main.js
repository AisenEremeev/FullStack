import './style.css';

document.querySelector('#app-title').textContent = import.meta.env.VITE_APP_TITLE || 'Full Stack Lab';
document.querySelector('#mode').textContent = import.meta.env.DEV ? 'Development' : 'Production build';
document.querySelector('#hmr-example').textContent = 'Готово к первому изменению.';

let count = 0;
const counter = document.querySelector('#counter');
document.querySelector('#increment').addEventListener('click', () => { counter.textContent = String(++count); });
document.querySelector('#reset').addEventListener('click', () => { count = 0; counter.textContent = '0'; });

const button = document.querySelector('#load');
const status = document.querySelector('#api-status');
const rows = document.querySelector('#customers');
button.addEventListener('click', async () => {
  button.disabled = true;
  rows.replaceChildren();
  status.textContent = 'Загрузка…';
  status.classList.remove('error');
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10000);
  try {
    const response = await fetch('/api/customers', { signal: controller.signal });
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const customers = await response.json();
    if (!Array.isArray(customers)) throw new Error('Ожидался массив клиентов');
    for (const customer of customers) {
      const row = document.createElement('tr');
      for (const value of [customer.id, customer.firstName, customer.lastName]) {
        const cell = document.createElement('td');
        cell.textContent = String(value ?? '');
        row.append(cell);
      }
      rows.append(row);
    }
    status.textContent = customers.length ? `Загружено клиентов: ${customers.length}` : 'База пуста. Добавь клиента через Postman в лабораторной № 3.';
  } catch (error) {
    status.classList.add('error');
    status.textContent = `Не удалось загрузить клиентов. Проверь запуск лабораторной № 3 на порту 8083. (${error.name === 'AbortError' ? 'Время ожидания истекло' : error.message})`;
  } finally {
    clearTimeout(timeout);
    button.disabled = false;
  }
});
