package com.example.chatbot.domain.document.service;

import com.example.chatbot.domain.document.dto.DocumentDto.DocumentResponse;
import com.example.chatbot.domain.document.entity.Document;
import com.example.chatbot.domain.document.entity.DocumentChunk;
import com.example.chatbot.domain.document.repository.DocumentChunkRepository;
import com.example.chatbot.domain.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final TikaService tikaService;
    private final EmbeddingService embeddingService; // ✅ 추가


    // ✅ 문서 업로드 + 파싱 + 청킹
    public Mono<DocumentResponse> uploadDocument(Long userId, String filename, InputStream inputStream) {

        Document document = Document.builder().userId(userId).filename(filename).status("PROCESSING").build();

        return documentRepository.save(document).flatMap(savedDoc -> {

            String text = tikaService.extractText(inputStream);
            List<String> chunks = tikaService.chunk(text);

            // 전부 Blocking
            // Netty Thread
            //    ↓
            //Tika 파싱

            log.info("문서 파싱 완료 filename={}, chunks={}", filename, chunks.size());

            AtomicInteger index = new AtomicInteger(0);

            return Flux.fromIterable(chunks).flatMap(chunk -> {
                DocumentChunk documentChunk = DocumentChunk.builder().documentId(savedDoc.getId()).content(chunk).chunkIndex(index.getAndIncrement()).build();
                return documentChunkRepository.save(documentChunk);
            }).then(documentRepository.findById(savedDoc.getId()).flatMap(doc -> {
                Document updated = Document.builder().id(doc.getId()).userId(doc.getUserId()).filename(doc.getFilename()).status("READY").createdAt(doc.getCreatedAt()).build();
                return documentRepository.save(updated);
            })).flatMap(doc ->
                    // ✅ 임베딩 생성 (비동기)
                    embeddingService.embedDocument(savedDoc.getId()).thenReturn(doc)).map(this::toResponse);
        });

    }


    // ✅ 문서 목록 조회
    public Flux<DocumentResponse> getDocuments(Long userId) {
        return documentRepository.findByUserId(userId).map(this::toResponse);
    }

    // ✅ Entity → DTO 변환
    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(document.getId(), document.getFilename(), document.getStatus(), document.getCreatedAt());
    }

    public Mono<DocumentResponse> saveDocumentInfo(Long userId, String filename, String filePath) {

        Document document = Document.builder().userId(userId).filename(filename).status("PROCESSING").build();

        return documentRepository.save(document).flatMap(savedDoc ->
                //Netty Thread
                //    ↓
                //작업 위임
                //    ↓
                //boundedElastic Thread
                //    ↓
                //파일 읽기 + Tika 파싱
                Mono.fromCallable(() -> {

                            try (InputStream inputStream = new FileInputStream(filePath)) {

                                String text = tikaService.extractText(inputStream);

                                return tikaService.chunk(text);
                            }
                        }).subscribeOn(Schedulers.boundedElastic())
                        .flatMapMany(chunks -> {

                            AtomicInteger index = new AtomicInteger(0);

                            return Flux.fromIterable(chunks).flatMap(chunk -> {

                                DocumentChunk documentChunk = DocumentChunk.builder().documentId(savedDoc.getId()).content(chunk).chunkIndex(index.getAndIncrement()).build();

                                return documentChunkRepository.save(documentChunk);
                            });
                        })

                        .then(updateReadyStatus(savedDoc.getId()))

                        .flatMap(doc -> embeddingService.embedDocument(savedDoc.getId()).thenReturn(doc))

                        .map(this::toResponse));
    }
    private Mono<Document> updateReadyStatus(Long documentId) {

        return documentRepository.findById(documentId).flatMap(doc -> {

            Document updated = Document.builder().id(doc.getId()).userId(doc.getUserId()).filename(doc.getFilename()).status("READY").createdAt(doc.getCreatedAt()).build();

            return documentRepository.save(updated);
        });
    }
}
