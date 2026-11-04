package com.bootcamp.customer_service.entity;

import com.bootcamp.common.entity.BaseEntity;
import com.bootcamp.customer_service.entity.enums.DocumentType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Metadata only. The file itself is uploaded to external storage; storageKey points to it.
@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private String fileName;

    private String storageKey;

    private String contentType;

    private long sizeBytes;

    public Document(DocumentType documentType, String fileName, String storageKey, String contentType, long sizeBytes) {
        this.documentType = documentType;
        this.fileName = fileName;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    void assignTo(Customer customer) {
        this.customer = customer;
    }
}
