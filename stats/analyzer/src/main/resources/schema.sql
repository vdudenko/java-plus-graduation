CREATE TABLE IF NOT EXISTS interactions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    rating FLOAT NOT NULL,
    ts TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, event_id)
);

CREATE TABLE IF NOT EXISTS similarities (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event1 BIGINT NOT NULL,
    event2 BIGINT NOT NULL,
    similarity FLOAT NOT NULL,
    ts TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (event1, event2)
);

CREATE INDEX idx_int_user_event ON interactions(user_id, event_id);
CREATE INDEX idx_int_event ON interactions(event_id);
CREATE INDEX idx_sim_event1 ON similarities(event1);
CREATE INDEX idx_sim_event2 ON similarities(event2);