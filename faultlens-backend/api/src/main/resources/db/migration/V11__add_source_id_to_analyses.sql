ALTER TABLE analyses
ADD COLUMN source_id BIGINT;

-- Mevcut analizlerin source_id'lerini log_groups tablosuyla eşleştirerek güncelle
UPDATE analyses a
SET source_id = (SELECT lg.source_id FROM log_groups lg WHERE lg.id = a.log_group_id)
WHERE a.source_id IS NULL;
