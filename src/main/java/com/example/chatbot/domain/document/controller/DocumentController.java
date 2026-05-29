// src/main/java/com/example/chatbot/domain/document/controller/DocumentController.java

package com.example.chatbot.domain.document.controller;

import com.example.chatbot.domain.document.dto.DocumentDto.DocumentResponse;
import com.example.chatbot.domain.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // ✅ 파일 업로드
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DocumentResponse> uploadDocument(
            @RequestPart("file") FilePart filePart
    ) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMap(userId -> {

                    String filename =
                            UUID.randomUUID() + "_" +
                                    filePart.filename();

                    Path path = Paths.get("uploads")
                            .resolve(filename);

                    try {
                        Files.createDirectories(path.getParent());
                    } catch (IOException e) {
                        return Mono.error(e);
                    }

                    return DataBufferUtils.write(
                                    filePart.content(),
                                    path,
                                    StandardOpenOption.CREATE
                            )
                            .then(
                                    documentService.saveDocumentInfo(
                                            userId,
                                            filePart.filename(),
                                            path.toString()
                                    )
                            );
                });
    }


    // ✅ 문서 목록 조회
    @GetMapping
    public Flux<DocumentResponse> getDocuments() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (Long) ctx.getAuthentication().getPrincipal())
                .flatMapMany(userId -> documentService.getDocuments(userId));
    }
}
