package com.example.thirdtool.infra.adapter;

import org.springframework.web.multipart.MultipartFile;


public interface FileStoragePort {
    String uploadFile(MultipartFile file, String folderPath);

    void deleteFile(String fileUrl);
}