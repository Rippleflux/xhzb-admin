package com.xhzb.nursing.config;

import com.xhzb.nursing.constants.SystemConstants;
import com.xhzb.nursing.service.impl.RedisChatMemoryServiceImpl;
import com.xhzb.nursing.tools.NursingTools;
import com.xhzb.nursing.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SpringAiConfig {

    /**
     * 创建一个OpenAiChatModel对象，装入到Spring容器中
     *
     * @return OpenAiChatModel对象
     */
    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel chatModel, WeatherTools weatherTools, NursingTools nursingTools, RedisChatMemoryServiceImpl chatMemory) {
        return ChatClient.builder(chatModel)
                //.defaultSystem(SystemConstants.prompt)
                .defaultSystem(SystemConstants.xhzbPrompt)
                .defaultTools(weatherTools, nursingTools)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(), //添加默认的advisor 记录日志
                        MessageChatMemoryAdvisor.builder(chatMemory).build() // 聊天记忆通知
                )
                .build();
    }

    @Bean
    @Primary
    public ChatClient openAiRagChatClient(OpenAiChatModel openAiChatModel, VectorStore vectorStore, ChatMemory redisChatMemoryService) {

        // 检索rag的数据
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor
                .builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .similarityThreshold(0.7d)
                        .topK(5)
                        .build())
                .build();

        return ChatClient
                .builder(openAiChatModel)
                .defaultSystem("你的名字叫小智，专门为养老院的员工进行服务，回答要特别客气！")
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(redisChatMemoryService).build(),
                        questionAnswerAdvisor)
                .build();
    }

    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    @Bean
    public ChatClient deepSeekChatClient(DeepSeekChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel).build();
    }

    @Bean
    public TextSplitter textSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(500)  //目标块大小  token数
                .withMinChunkSizeChars(200) // 最小块的字符数
                .withMinChunkLengthToEmbed(10) // 最小的文本字符长度
                .withMaxNumChunks(10000)  //文档最大块数
                .withKeepSeparator(false)   //不保留换行符
                .build();
    }

    @Bean
    public ChatClient chatClientByAssessment(OpenAiChatModel openAiChatModel) {

        return ChatClient
                .builder(openAiChatModel)
                .defaultSystem("你是一个健康评估专家，专门用来评估老人的健康情况")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }


}