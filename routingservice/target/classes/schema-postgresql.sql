CREATE INDEX IF NOT EXISTS idx_stations_name ON stations USING gin (to_tsvector('simple', name));
CREATE INDEX IF NOT EXISTS idx_timetables_days ON timetables USING gin (days_of_week);
