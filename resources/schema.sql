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
  category TEXT,
  description TEXT,
  parent_id BLOB,
  FOREIGN KEY(parent_id) REFERENCES collection(id),
  UNIQUE(name)
) STRICT;

CREATE TABLE item_type (
  id BLOB PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  UNIQUE(name)
) STRICT;

CREATE TABLE person (
  id BLOB PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  notes TEXT
) STRICT;

CREATE TABLE saved_search (
  id BLOB PRIMARY KEY NOT NULL,
  created_at INT NOT NULL,
  name TEXT NOT NULL,
  date_from INT,
  date_to INT,
  description TEXT,
  UNIQUE(name)
) STRICT;

CREATE TABLE subject (
  id BLOB PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  parent_id BLOB,
  FOREIGN KEY(parent_id) REFERENCES subject(id),
  UNIQUE(name)
) STRICT;

CREATE TABLE tag (
  id BLOB PRIMARY KEY NOT NULL,
  name TEXT NOT NULL,
  UNIQUE(name)
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
  item_type_id BLOB NOT NULL,
  published INT NOT NULL,
  title TEXT NOT NULL,
  updated_at INT NOT NULL,
  date_display TEXT,
  date_end INT,
  date_start INT,
  description TEXT,
  notes TEXT,
  rights TEXT,
  source TEXT,
  FOREIGN KEY(item_type_id) REFERENCES item_type(id)
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

CREATE TABLE item_subject (
  id BLOB PRIMARY KEY NOT NULL,
  item_id BLOB NOT NULL,
  subject_id BLOB NOT NULL,
  FOREIGN KEY(item_id) REFERENCES item(id),
  FOREIGN KEY(subject_id) REFERENCES subject(id),
  UNIQUE(subject_id, item_id)
) STRICT;

CREATE TABLE item_tag (
  id BLOB PRIMARY KEY NOT NULL,
  item_id BLOB NOT NULL,
  tag_id BLOB NOT NULL,
  FOREIGN KEY(item_id) REFERENCES item(id),
  FOREIGN KEY(tag_id) REFERENCES tag(id),
  UNIQUE(tag_id, item_id)
) STRICT;

CREATE INDEX idx_collection_category ON collection(category);
CREATE INDEX idx_collection_parent_id ON collection(parent_id);
CREATE INDEX idx_file_item_id ON file(item_id);
CREATE INDEX idx_file_sha256 ON file(sha256);
CREATE INDEX idx_item_date_start ON item(date_start);
CREATE INDEX idx_item_item_type_id ON item(item_type_id);
CREATE INDEX idx_item_collection_collection_id ON item_collection(collection_id);
CREATE INDEX idx_item_collection_item_id ON item_collection(item_id);
CREATE INDEX idx_item_person_item_id ON item_person(item_id);
CREATE INDEX idx_item_person_person_id ON item_person(person_id);
CREATE INDEX idx_item_subject_item_id ON item_subject(item_id);
CREATE INDEX idx_item_subject_subject_id ON item_subject(subject_id);
CREATE INDEX idx_item_tag_item_id ON item_tag(item_id);
CREATE INDEX idx_item_tag_tag_id ON item_tag(tag_id);
CREATE INDEX idx_person_name ON person(name);
CREATE INDEX idx_subject_parent_id ON subject(parent_id);
CREATE INDEX idx_user_joined_at ON user(joined_at);