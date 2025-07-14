package com.madcamp.moody.diary;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaryRequestDTO {
    private String content;
    private String date; // yyyy-MM-dd
}
