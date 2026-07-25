package com.xhzb;


import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
@SpringBootTest
public class ETLTest {

    @Autowired
    private VectorStore vectorStore;


    @Test
    public void testLoadText() throws FileNotFoundException {
        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\CodeFile\\java\\xhzb-parent-master\\sql\\xhzb.sql"));
        // 创建TextReader
        TextReader textReader = new TextReader(resource);

        //追加一点东西(自定义)
        textReader.getCustomMetadata().put("filename", "xhzb.sql");

        // 读取文件内容
        List<Document> documentList = textReader.read();
        for (Document document : documentList) {
            System.out.println(document.getFormattedContent());
        }
    }

    // 按页读取
    @Test
    public void testPDFByPage() throws FileNotFoundException {
        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\星海智伴\\day04-知识库(RAG)\\资料\\护理员工工作手册.pdf"));

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

        System.out.println(pdfReader.read());
    }
    // 按段落读取
    @Test
    public void testPDFByParagraph() throws FileNotFoundException {
        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\星海智伴\\day04-知识库(RAG)\\资料\\护理员工工作手册.pdf"));

        ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

        System.out.println(pdfReader.read());
    }

    @Test
    public void testTestSplitter() throws FileNotFoundException {

        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\星海智伴\\day04-知识库(RAG)\\资料\\护理员工工作手册.pdf"));

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());


        // 创建TextSplitter
        TextSplitter textSplitter = new TokenTextSplitter();
        System.out.println("分隔之前的文档数："+pdfReader.read().size());
        List<Document> documents = textSplitter.apply(pdfReader.read());
        System.out.println("分隔之后的文档数："+documents.size());
        System.out.println(documents);

    }

    @Test
    public void testTestTokenSplitter() throws FileNotFoundException {

        // 读取文件
        InputStreamResource resource = new InputStreamResource(new FileInputStream("E:\\星海智伴\\day04-知识库(RAG)\\资料\\护理员工工作手册.pdf"));

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0) // 设置页眉边距
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0) // 删除页眉顶部的文本行数
                                .build())
                        .withPagesPerDocument(1) // 每个文档的页数
                        .build());

        // 创建TextSplitter
        TextSplitter textSplitter = new TokenTextSplitter();
        System.out.println("分隔之前的文档数："+pdfReader.read().size());
        List<Document> documents = textSplitter.apply(pdfReader.read());
        System.out.println("分隔之后的文档数："+documents.size());

        //存储到向量数据库中，分批添加（每批最多10个）
        int batchSize = 10;
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<Document> batch = documents.subList(i, Math.min(i + batchSize, documents.size()));
            vectorStore.add(batch);
            System.out.println("已添加批次: " + (i / batchSize + 1) + ", 数量: " + batch.size());
        }
    }

    @Test
    public void testRetriever() {
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.4) // 设置相似度阈值(比较距离,距离越小越相近)
                .topK(5) // 设置返回的文档数量
                .build();
        List<Document> documents = retriever.retrieve(new Query("护理服务宗旨与核心价值是什么"));
        System.out.println(documents);
    }



}