package com.multi.mlpenterpriseapprovalsystem.search.infra;

import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Response;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.search.dto.SearchDocument;
import com.multi.mlpenterpriseapprovalsystem.search.web.dto.MenuConfig;
import com.multi.mlpenterpriseapprovalsystem.search.web.dto.SearchGroupedResponse;
import com.multi.mlpenterpriseapprovalsystem.search.web.dto.SearchPageResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ElasticsearchGateway
 * @since : 2026. 1. 9. 금요일
 */
@Component
public class ElasticsearchGateway {

    private final Rest5Client restClient;
    private final ObjectMapper om;
    private final ElasticsearchProps props;
    // 생성자를 직접 작성하여 주입 대상을 명확히 지정
    public ElasticsearchGateway(
            Rest5Client restClient,
            @Qualifier("esObjectMapper") ObjectMapper om,
            ElasticsearchProps props) {
        this.restClient = restClient;
        this.om = om;
        this.props = props;
    }

    public void bulkUpsert(List<SearchDocument> docs) throws IOException {
        if (docs.isEmpty()) return;

        StringBuilder ndjson = new StringBuilder();
        for (SearchDocument d : docs) {
            // action line
            ndjson.append("{\"index\":{\"_index\":\"")
                    .append(props.getIndexSearchAlias())
                    .append("\",\"_id\":\"")
                    .append(escapeJson(d.getDocId()))
                    .append("\"}}\n");
            // source line
            ndjson.append(om.writeValueAsString(d)).append("\n");
        }

        Request req = new Request("POST", "/_bulk");
        req.addParameter("refresh", "false");
        req.setJsonEntity(ndjson.toString());
        Response resp = restClient.performRequest(req);

        Map<String, Object> parsed = parseJson(resp);
        Object errors = parsed.get("errors");
        if (Boolean.TRUE.equals(errors)) {
            throw new IOException("ES bulkUpsert has errors: " + om.writeValueAsString(parsed));
        }
    }

    public void bulkDelete(List<String> docIds) throws IOException {
        if (docIds.isEmpty()) return;

        StringBuilder ndjson = new StringBuilder();
        for (String id : docIds) {
            ndjson.append("{\"delete\":{\"_index\":\"")
                    .append(props.getIndexSearchAlias())
                    .append("\",\"_id\":\"")
                    .append(escapeJson(id))
                    .append("\"}}\n");
        }

        Request req = new Request("POST", "/_bulk");
        req.addParameter("refresh", "false");
        req.setJsonEntity(ndjson.toString());
        Response resp = restClient.performRequest(req);

        Map<String, Object> parsed = parseJson(resp);
        Object errors = parsed.get("errors");
        if (Boolean.TRUE.equals(errors)) {
            throw new IOException("ES bulkDelete has errors: " + om.writeValueAsString(parsed));
        }
    }

    public Optional<MenuConfig> getMenuConfig() throws IOException {
        Request req = new Request("GET", "/" + props.getIndexMenuAlias() + "/_doc/GLOBAL");
        try {
            Response resp = restClient.performRequest(req);

            Map<String, Object> parsed = parseJson(resp);
            Map<String, Object> src = (Map<String, Object>) parsed.get("_source");
            if (src == null) return Optional.empty();

            return Optional.of(om.convertValue(src, MenuConfig.class));

        } catch (ResponseException e) {
            int status = e.getResponse().getStatusCode();
            if (status == 404) return Optional.empty();
            throw e;
        }
    }

    /**
     * 메뉴별(섹션별) filters agg + top_hits 한방 검색
     * - title/content 하이라이트가 "제목/내용 둘 다" 예쁘게 나오도록 highlight 옵션 추가
     */
    public SearchGroupedResponse searchGrouped(String comId, String query, List<String> aclTokens, MenuConfig menuConfig) throws IOException {

        // ✅ 메뉴 기반 filters 생성 (v4: type은 keyword 단일필드 → type.keyword 금지)
        Map<String, Object> filters = new LinkedHashMap<>();
        menuConfig.getMenus().stream()
                .sorted(Comparator.comparingInt(MenuConfig.Menu::getOrder))
                .forEach(m -> {
                    Map<String, Object> terms = Map.of("terms", Map.of("type", m.getTypes())); // ✅ FIX
                    filters.put(m.getMenuKey(), terms);
                });

        int defaultSize = 5;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("size", 0);

        // ===== query.bool =====
        String q = (query == null) ? "" : query.trim();

        Map<String, Object> bool = new LinkedHashMap<>();

        // ✅ filter: company + acl (v4: companyId/acl은 keyword 단일필드 → .keyword 금지)
        List<Object> filterList = new ArrayList<>();
        filterList.add(Map.of("term", Map.of("companyId", comId))); // ✅ FIX

        if (aclTokens != null && !aclTokens.isEmpty()) {
            filterList.add(Map.of("terms", Map.of("acl", aclTokens))); // ✅ FIX
        }
        bool.put("filter", filterList);

        if (q.isEmpty()) {
            bool.put("must", List.of(Map.of("match_all", Map.of())));
        } else {
            // ✅ ngram + nori 혼합 검색
            Map<String, Object> mm = Map.of(
                    "multi_match", Map.of(
                            "query", q,
                            "type", "best_fields",
                            "operator", "or",
                            "fields", List.of(
                                    "title^4",
                                    "title.ngram^2",
                                    "summary^2",
                                    "summary.ngram",
                                    "contentText",
                                    "contentText.ngram"
                            )
                    )
            );
            bool.put("must", List.of(mm));
        }

        body.put("query", Map.of("bool", bool));

        // ===== highlight =====
        Map<String, Object> highlight = new LinkedHashMap<>();
        highlight.put("pre_tags", List.of("<em>"));
        highlight.put("post_tags", List.of("</em>"));
        highlight.put("require_field_match", false);

        Map<String, Object> hlFields = new LinkedHashMap<>();
        hlFields.put("title", Map.of(
                "number_of_fragments", 0,
                "matched_fields", List.of("title", "title.ngram")
        ));
        hlFields.put("contentText", Map.of(
                "matched_fields", List.of("contentText", "contentText.ngram"),
                "fragment_size", 140,
                "number_of_fragments", 1,
                "no_match_size", 140
        ));
        hlFields.put("summary", Map.of(
                "matched_fields", List.of("summary", "summary.ngram"),
                "fragment_size", 140,
                "number_of_fragments", 1,
                "no_match_size", 140
        ));
        highlight.put("fields", hlFields);

        // ===== aggs: menus(filters) + top_hits =====
        Map<String, Object> topHitsAgg = new LinkedHashMap<>();
        topHitsAgg.put("top_hits", Map.of(
                "size", defaultSize,
                "sort", List.of(
                        Map.of("_score", Map.of("order", "desc")),
                        Map.of("updatedAt", Map.of("order", "desc", "missing", "_last"))
                ),
                "_source", Map.of("includes", List.of(
                        "type", "sourceId", "title", "summary", "contentText",
                        "createdAt", "updatedAt", "url","scope"
                )),
                "highlight", highlight
        ));

        Map<String, Object> aggs = new LinkedHashMap<>();
        aggs.put("menus", Map.of(
                "filters", Map.of("filters", filters),
                "aggs", Map.of("top", topHitsAgg)
        ));
        body.put("aggs", aggs);

        Request req = new Request("POST", "/" + props.getIndexSearchAlias() + "/_search");
        req.setJsonEntity(om.writeValueAsString(body));
        Response resp = restClient.performRequest(req);

        Map<String, Object> parsed = parseJson(resp);
        return SearchGroupedResponse.fromEsResponse(menuConfig, parsed);
    }

    public SearchPageResponse searchPage(
            String comId,
            String query,
            List<String> aclTokens,
            List<String> types,   // ["NOTICE","BOARD"] 처럼
            String scope,         // 일정이면 "PERSONAL/DEPARTMENT/COMPANY" 아니면 null
            int page,
            int size
    ) throws IOException {

        int from = Math.max(page, 0) * Math.max(size, 1);
        String q = (query == null) ? "" : query.trim();

        Map<String, Object> bool = new LinkedHashMap<>();
        List<Object> filterList = new ArrayList<>();
        filterList.add(Map.of("term", Map.of("companyId", comId)));

        if (aclTokens != null && !aclTokens.isEmpty()) {
            filterList.add(Map.of("terms", Map.of("acl", aclTokens)));
        }
        if (types != null && !types.isEmpty()) {
            filterList.add(Map.of("terms", Map.of("type", types)));
        }
        // ✅ 일정 scope 필터(옵션)
        if (scope != null && !scope.isBlank()) {
            filterList.add(Map.of("term", Map.of("scope", scope)));
        }
        bool.put("filter", filterList);

        if (q.isEmpty()) {
            bool.put("must", List.of(Map.of("match_all", Map.of())));
        } else {
            bool.put("must", List.of(
                    Map.of("multi_match", Map.of(
                            "query", q,
                            "type", "best_fields",
                            "operator", "or",
                            "fields", List.of(
                                    "title^4", "title.ngram^2",
                                    "summary^2", "summary.ngram",
                                    "contentText", "contentText.ngram"
                            )
                    ))
            ));
        }

        Map<String, Object> highlight = new LinkedHashMap<>();
        highlight.put("pre_tags", List.of("<em>"));
        highlight.put("post_tags", List.of("</em>"));
        highlight.put("require_field_match", false);

        Map<String, Object> hlFields = new LinkedHashMap<>();
        hlFields.put("title", Map.of("number_of_fragments", 0, "matched_fields", List.of("title", "title.ngram")));
        hlFields.put("contentText", Map.of("matched_fields", List.of("contentText", "contentText.ngram"),
                "fragment_size", 140, "number_of_fragments", 1, "no_match_size", 140));
        hlFields.put("summary", Map.of("matched_fields", List.of("summary", "summary.ngram"),
                "fragment_size", 140, "number_of_fragments", 1, "no_match_size", 140));
        highlight.put("fields", hlFields);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("size", size);
        body.put("query", Map.of("bool", bool));
        body.put("sort", List.of(
                Map.of("_score", Map.of("order", "desc")),
                Map.of("updatedAt", Map.of("order", "desc", "missing", "_last"))
        ));
        body.put("_source", Map.of("includes", List.of(
                "type","sourceId","title","summary","contentText","createdAt","updatedAt","url","scope"
        )));
        body.put("highlight", highlight);

        Request req = new Request("POST", "/" + props.getIndexSearchAlias() + "/_search");
        req.setJsonEntity(om.writeValueAsString(body));
        Response resp = restClient.performRequest(req);

        Map<String, Object> parsed = parseJson(resp);

        // total
        Map<String, Object> hits = (Map<String, Object>) parsed.get("hits");
        Map<String, Object> totalObj = (Map<String, Object>) hits.get("total");
        long total = totalObj == null ? 0 : ((Number) totalObj.getOrDefault("value", 0)).longValue();

        // items
        List<Map<String, Object>> hitList = (List<Map<String, Object>>) hits.get("hits");
        List<SearchGroupedResponse.Item> items = new ArrayList<>();

        double maxScore = 0.0;
        for (Map<String, Object> h : hitList) {
            double sc = h.get("_score") == null ? 0.0 : ((Number) h.get("_score")).doubleValue();
            if (sc > maxScore) maxScore = sc;

            Map<String, Object> src = (Map<String, Object>) h.get("_source");
            Map<String, Object> hl = (Map<String, Object>) h.get("highlight");

            SearchGroupedResponse.Item it = new SearchGroupedResponse.Item();
            it.setType((String) src.get("type"));
            it.setSourceId(String.valueOf(src.get("sourceId")));
            it.setTitle((String) src.get("title"));
            it.setSummary((String) src.get("summary"));
            it.setUrl((String) src.get("url"));
            it.setScore(sc);

            // ✅ 추가: 날짜 필드 매핑
            if (src.get("createdAt") != null) {
                it.setCreatedAt(((Number) src.get("createdAt")).longValue());
            }

            // ✅ scope 내려주고 싶으면 Item에 scope 필드 추가해야 함(아래 참고)
             it.setScope((String) src.get("scope"));

            if (hl != null) {
                Map<String, List<String>> highlightMap = new LinkedHashMap<>();
                for (Map.Entry<String, Object> e : hl.entrySet()) {
                    highlightMap.put(e.getKey(), (List<String>) e.getValue());
                }
                it.setHighlight(highlightMap);
            }
            items.add(it);
        }

        if (maxScore > 0) {
            for (SearchGroupedResponse.Item it : items) {
                it.setAccuracy((int) Math.round((it.getScore() / maxScore) * 100.0));
            }
        }

        return SearchPageResponse.builder()
                .query(q)
                .total(total)
                .page(page)
                .size(size)
                .items(items)
                .build();
    }
    private Map<String, Object> parseJson(Response resp) throws IOException {
        byte[] bytes = resp.getEntity().getContent().readAllBytes();
        return om.readValue(new String(bytes, StandardCharsets.UTF_8), new TypeReference<>() {});
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}