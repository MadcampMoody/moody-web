package com.madcamp.moody.groq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public GroqDTO.MusicAnalysisResponse recommendMusic(
            @RequestBody GroqDTO.SimpleRequest request,
            @AuthenticationPrincipal OAuth2User user
    ) {
        // 이제 user 객체를 통해 현재 로그인한 사용자 정보를 알 수 있습니다.
        // 예를 들어, 사용자 이름을 로그로 출력해볼 수 있습니다.
        System.out.println("음악 추천 요청 사용자: " + user.getName());
        return groqService.analyzeTextAndRecommendMusic(request.getPrompt(), request.getDate(), user);
    }
} 