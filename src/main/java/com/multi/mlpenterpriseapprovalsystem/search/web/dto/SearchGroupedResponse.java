package com.multi.mlpenterpriseapprovalsystem.search.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchGroupedResponse
 * @since : 2026. 1. 9. 금요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchGroupedResponse {

    private String query;
    private List<Section> sections = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section {
        private String menuKey;
        private String title;
        private long count;
        private List<Item> items = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String type;
        private String sourceId;
        private String title;
        private String summary;
        private String url;

        private Long createdAt; // ✅ 추가: 작성일

        private String scope; // ✅ 추가 (PERSONAL/DEPARTMENT/COMPANY)

        private Map<String, List<String>> highlight;
        private double score;
        private Integer accuracy; // UI용(선택)
    }

    @SuppressWarnings("unchecked")
    public static SearchGroupedResponse fromEsResponse(MenuConfig cfg, Map<String, Object> es) {
        SearchGroupedResponse out = new SearchGroupedResponse();
        out.setQuery(extractQuery(es));

        Map<String, Object> aggs = (Map<String, Object>) es.get("aggregations");
        Map<String, Object> menus = (Map<String, Object>) aggs.get("menus");
        Map<String, Object> buckets = (Map<String, Object>) menus.get("buckets");

        // 메뉴 순서대로 섹션 생성
        cfg.getMenus().stream()
                .sorted(Comparator.comparingInt(MenuConfig.Menu::getOrder))
                .forEach(m -> {
                    Map<String, Object> b = (Map<String, Object>) buckets.get(m.getMenuKey());
                    if (b == null) return;

                    long count = ((Number) b.getOrDefault("doc_count", 0)).longValue();

                    Map<String, Object> top = (Map<String, Object>) b.get("top");
                    Map<String, Object> hits = (Map<String, Object>) top.get("hits");
                    List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");

                    List<Item> items = new ArrayList<>();
                    double maxScore = 0.0;
                    for (Map<String, Object> h : hitList) {
                        double score = h.get("_score") == null ? 0.0 : ((Number) h.get("_score")).doubleValue();
                        if (score > maxScore) maxScore = score;

                        Map<String, Object> src = (Map<String, Object>) h.get("_source");
                        Map<String, Object> hl = (Map<String, Object>) h.get("highlight");

                        Item item = new Item();
                        item.setType((String) src.get("type"));
                        item.setSourceId(String.valueOf(src.get("sourceId")));
                        item.setTitle((String) src.get("title"));
                        item.setSummary((String) src.get("summary"));
                        item.setUrl((String) src.get("url"));
                        item.setScope((String) src.get("scope")); // ✅ 추가

                        // ✅ 추가: 작성일/수정일 매핑
                        if (src.get("createdAt") != null) {
                            item.setCreatedAt(((Number) src.get("createdAt")).longValue());
                        }

                        if (hl != null) {
                            Map<String, List<String>> highlight = new LinkedHashMap<>();
                            for (Map.Entry<String, Object> e : hl.entrySet()) {
                                highlight.put(e.getKey(), (List<String>) e.getValue());
                            }
                            item.setHighlight(highlight);
                        }

                        items.add(item);
                    }

                    // accuracy(선택): 섹션 내 score 상대값
                    if (maxScore > 0) {
                        for (Item it : items) {
                            int acc = (int) Math.round((it.getScore() / maxScore) * 100.0);
                            it.setAccuracy(acc);
                        }
                    }

                    Section section = new Section(m.getMenuKey(), m.getTitle(), count, items);
                    out.getSections().add(section);
                });

        return out;
    }

    private static String extractQuery(Map<String, Object> es) {
        // 응답에서 query를 뽑긴 애매해서 컨트롤러에서 세팅하는게 정석.
        return null;
    }
}
