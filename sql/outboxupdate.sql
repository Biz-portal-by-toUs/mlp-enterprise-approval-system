use bizportal;

INSERT INTO search_outbox (company_id, doc_type, source_id, op, schema_ver, status, retry_count, occurred_at)
SELECT b.com_id, 'BOARD', CAST(b.board_no AS CHAR), 'UPSERT', 1, 'PENDING', 0, NOW()
FROM board b
WHERE b.is_deleted = 0;
INSERT INTO search_outbox (company_id, doc_type, source_id, op, schema_ver, status, retry_count, occurred_at)
SELECT s.com_id, 'SCHEDULE', CAST(s.sch_no AS CHAR), 'UPSERT', 1, 'PENDING', 0, NOW()
FROM schedule s;
INSERT INTO search_outbox (company_id, doc_type, source_id, op, schema_ver, status, retry_count, occurred_at)
SELECT d.com_id, 'APPROVAL', CAST(d.doc_no AS CHAR), 'UPSERT', 1, 'PENDING', 0, NOW()
FROM document d
WHERE UPPER(d.doc_stat) = 'FI';

SELECT status, COUNT(*) FROM search_outbox GROUP BY status;

SELECT doc_stat, COUNT(*) FROM document GROUP BY doc_stat;

SHOW CREATE TABLE document;

SELECT status, COUNT(*) cnt
FROM search_outbox
GROUP BY status;