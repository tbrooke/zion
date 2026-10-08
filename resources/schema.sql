-- Auto-generated; do not edit.

CREATE TABLE biff_sqlite_kv (
  id BLOB PRIMARY KEY NOT NULL,
  k TEXT NOT NULL,
  namespace TEXT NOT NULL,
  v BLOB NOT NULL,
  UNIQUE(namespace, k)
) STRICT;

CREATE TABLE collection (
  id BLOB PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  UNIQUE(name)
) STRICT;

CREATE TABLE era (
  id BLOB PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  end_year INT,
  start_year INT,
  UNIQUE(name)
) STRICT;

CREATE TABLE person (
  id BLOB PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  notes TEXT
) STRICT;

CREATE TABLE user (
  id BLOB PRIMARY KEY NOT NULL,
  email TEXT NOT NULL,
  joined_at INT NOT NULL,
  display_name TEXT,
  UNIQUE(email)
) STRICT;

CREATE TABLE item (
  id BLOB PRIMARY KEY NOT NULL,
  created_at INT NOT NULL,
  kind INT NOT NULL CHECK (kind IN (1, 2, 3, 4, 5, 6, 7, 8)), -- photograph (1), bulletin (2), newsletter (3), letter (4), minutes (5), program (6), document (7), other (8)
  published INT NOT NULL,
  title TEXT NOT NULL,
  updated_at INT NOT NULL,
  date_display TEXT,
  date_end INT,
  date_start INT,
  description TEXT,
  era_id BLOB,
  notes TEXT,
  rights TEXT,
  source TEXT,
  FOREIGN KEY(era_id) REFERENCES era(id)
) STRICT;

CREATE TABLE file (
  id BLOB PRIMARY KEY NOT NULL,
  created_at INT NOT NULL,
  item_id BLOB NOT NULL,
  mime_type TEXT NOT NULL,
  position INT NOT NULL,
  sha256 TEXT NOT NULL,
  size_bytes INT NOT NULL,
  storage_key TEXT NOT NULL,
  original_name TEXT,
  FOREIGN KEY(item_id) REFERENCES item(id),
  UNIQUE(storage_key)
) STRICT;

CREATE TABLE item_collection (
  id BLOB PRIMARY KEY NOT NULL,
  collection_id BLOB NOT NULL,
  item_id BLOB NOT NULL,
  FOREIGN KEY(collection_id) REFERENCES collection(id),
  FOREIGN KEY(item_id) REFERENCES item(id),
  UNIQUE(collection_id, item_id)
) STRICT;

CREATE TABLE item_person (
  id BLOB PRIMARY KEY NOT NULL,
  item_id BLOB NOT NULL,
  person_id BLOB NOT NULL,
  role INT NOT NULL CHECK (role IN (1, 2, 3, 4)), -- depicted (1), creator (2), recipient (3), contributor (4)
  FOREIGN KEY(item_id) REFERENCES item(id),
  FOREIGN KEY(person_id) REFERENCES person(id),
  UNIQUE(role, item_id, person_id)
) STRICT;

CREATE INDEX idx_file_item_id ON file(item_id);
CREATE INDEX idx_file_sha256 ON file(sha256);
CREATE INDEX idx_item_date_start ON item(date_start);
CREATE INDEX idx_item_era_id ON item(era_id);
CREATE INDEX idx_item_kind ON item(kind);
CREATE INDEX idx_item_collection_collection_id ON item_collection(collection_id);
CREATE INDEX idx_item_collection_item_id ON item_collection(item_id);
CREATE INDEX idx_item_person_item_id ON item_person(item_id);
CREATE INDEX idx_item_person_person_id ON item_person(person_id);
CREATE INDEX idx_person_name ON person(name);
CREATE INDEX idx_user_joined_at ON user(joined_at);