package com.madcamp.moody.groq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groq")
@CrossOrigin(origins = "*")
public class GroqController {
    
    private final GroqService groqService;
    
    @Autowired
    public GroqController(GroqService groqService) {
        this.groqService = groqService;
    }
    

    
    // 텍스트 분석 + 음악 추천
    @PostMapping("/recommend-music")
    public GroqDTO.MusicAnalysisResponse recommendMusic(@RequestBody GroqDTO.SimpleRequest request) {
        return groqService.analyzeTextAndRecommendMusic(request.getPrompt());
    }
} 