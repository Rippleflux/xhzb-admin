package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.util.List;

import cn.hutool.json.JSONUtil;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.KnowledgeBaseMapper;
import com.xhzb.nursing.domain.KnowledgeBase;
import com.xhzb.nursing.service.IKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 知识库Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-12
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService
{
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;


    @Autowired
    private OSSAliyunFileStorageService fileStorageService;

    @Autowired
    private TextSplitter textSplitter;

    @Autowired
    private VectorStore vectorStore;
    /**
     * 查询知识库
     * 
     * @param id 知识库主键
     * @return 知识库
     */
    @Override
    public KnowledgeBase selectKnowledgeBaseById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询知识库列表
     * 
     * @param knowledgeBase 知识库
     * @return 知识库
     */
    @Override
    public List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase)
    {
        return knowledgeBaseMapper.selectKnowledgeBaseList(knowledgeBase);
    }



    /**
     * 新增知识库主
     *
     * @param knowledgeBase 知识库主
     * @return 结果
     */
    @Override
    public int insertKnowledgeBase(KnowledgeBase knowledgeBase) {
        // 下载文件
        InputStream inputStream = fileStorageService.download(knowledgeBase.getDocumentUrl());
        if(inputStream == null){
            throw new ServiceException("上传的文件不存在");
        }

        // 读取PDF
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new InputStreamResource(inputStream),
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
                        .build()
        );

        // 对PDF进行拆分
        List<Document> documentList = textSplitter.split(pdfReader.read());

        // 分批次存储到向量数据库
        int batchSize = 10;
        for (int i = 0; i < documentList.size(); i += batchSize) {
            List<Document> batch = documentList.subList(i, Math.min(i + batchSize, documentList.size()));
            // 3.写入向量库
            vectorStore.add(batch);
            System.out.println("已添加批次: " + (i / batchSize + 1) + ", 数量: " + batch.size());
        }

        //获取所有的文档的id
        List<String> documentIds = documentList.stream().map(Document::getId).toList();

        // 保存知识库文档到数据库
        knowledgeBase.setCreateTime(DateUtils.getNowDate());
        knowledgeBase.setRemark(JSONUtil.toJsonStr(documentIds));
        return knowledgeBaseMapper.insertKnowledgeBase(knowledgeBase);
    }

    /**
     * 修改知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int updateKnowledgeBase(KnowledgeBase knowledgeBase)
    {
        return updateById(knowledgeBase)? 1 : 0;
    }


    /**
     * 批量删除知识库
     *
     * @param id 需要删除的知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseById(Long id)
    {
        //查数据
        KnowledgeBase knowledgeBase = selectKnowledgeBaseById(id);
        if(null == knowledgeBase){
            throw new BaseException("知识库不存在");
        }
        // 删除向量中的数据
        String idsStr = knowledgeBase.getRemark();
        List<String> ids = JSONUtil.toList(idsStr, String.class);
        vectorStore.delete(ids);
        //OSS中的数据 也要删除
        fileStorageService.delete(knowledgeBase.getDocumentUrl());
        // 删除mysql的数据
        return knowledgeBaseMapper.deleteKnowledgeBaseById(id);
    }


}
