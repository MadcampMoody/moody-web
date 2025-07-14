package com.madcamp.moody.mood;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MoodDTO {
    private String moodType;
    private String emoji;
    private String name;
    private String date;

    public MoodDTO(Mood mood) {
        this.moodType = mood.getMoodType().name();
        this.date = mood.getDate().toString(); // LocalDate를 String으로 변환
        switch (mood.getMoodType()) {
            case ANNOYED:
                this.emoji = "😤"; this.name = "짜증나요"; break;
            case ANGRY:
                this.emoji = "😡"; this.name = "화나요"; break;
            case TIRED:
                this.emoji = "😴"; this.name = "피곤해요"; break;
            case SAD:
                this.emoji = "😢"; this.name = "슬퍼요"; break;
            case WORRIED:
                this.emoji = "😟"; this.name = "걱정돼요"; break;
            case BORED:
                this.emoji = "😒"; this.name = "지루해요"; break;
            case HAPPY:
                this.emoji = "😊"; this.name = "행복해요"; break;
            case CALM:
                this.emoji = "😌"; this.name = "침착해요"; break;
            case EXCITED:
                this.emoji = "😃"; this.name = "신나요"; break;
            case PROUD:
                this.emoji = "😎"; this.name = "자랑스러워요"; break;
            case THANKFUL:
                this.emoji = "😊"; this.name = "감사해요"; break;
            default:
                this.emoji = ""; this.name = ""; break;
        }
    }
}
