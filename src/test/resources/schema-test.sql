-- Создаем последовательность для H2 (для совместимости, хотя в тестах используется fallback)
CREATE SEQUENCE IF NOT EXISTS document_number_seq START WITH 1 INCREMENT BY 1;
