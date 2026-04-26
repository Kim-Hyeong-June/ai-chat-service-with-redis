-- src/main/resources/db/migration/V1__init.sql

-- ✅ pgvector 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- ✅ 1. 사용자
CREATE TABLE users (
                       id          BIGSERIAL PRIMARY KEY,
                       email       VARCHAR(255) NOT NULL UNIQUE,
                       password    VARCHAR(255) NOT NULL,
                       name        VARCHAR(100),
                       created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ✅ 2. 대화방
CREATE TABLE conversations (
                               id          BIGSERIAL PRIMARY KEY,
                               user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               title       VARCHAR(255),
                               created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                               updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ✅ 3. 메시지
CREATE TABLE messages (
                          id                  BIGSERIAL PRIMARY KEY,
                          conversation_id     BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                          role                VARCHAR(20) NOT NULL,   -- 'user' | 'assistant' | 'system'
                          content             TEXT NOT NULL,
                          tokens              INT,
                          created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ✅ 4. 문서 (RAG용)
CREATE TABLE documents (
                           id          BIGSERIAL PRIMARY KEY,
                           user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           filename    VARCHAR(255) NOT NULL,
                           file_path   VARCHAR(500),
                           status      VARCHAR(20) NOT NULL DEFAULT 'UPLOADING',  -- UPLOADING | PROCESSING | READY | FAILED
                           created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ✅ 5. 청크 (pgvector)
CREATE TABLE document_chunks (
                                 id              BIGSERIAL PRIMARY KEY,
                                 document_id     BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                 content         TEXT NOT NULL,
                                 embedding       vector(1536),       -- OpenAI ada-002 기준
                                 chunk_index     INT,
                                 metadata        JSONB,              -- 페이지번호 등
                                 created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ✅ 6. 대화 ↔ 문서 연결
CREATE TABLE conversation_documents (
                                        conversation_id     BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                                        document_id         BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                        PRIMARY KEY (conversation_id, document_id)
);

-- ✅ 인덱스
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);

-- ✅ 벡터 인덱스 (유사도 검색 성능)
CREATE INDEX idx_document_chunks_embedding
    ON document_chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
