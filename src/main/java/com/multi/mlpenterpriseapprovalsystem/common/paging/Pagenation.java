package com.multi.mlpenterpriseapprovalsystem.common.paging;

public class Pagenation {

    public static SelectCriteria getSelectCriteria(int pageNo, int totalCount, int limit, int buttonAmount) {
//        buttonAmount: 한 번에 화면에 보여줄 페이지 버튼의 수를 의미
//
//        예를 들어, 한 번에 10개의 페이지 버튼만 보여주고 싶다면 buttonAmount를 10으로 설정
//        페이지가 많을 때, 페이지 번호가 길게 나열되지 않고 적정 수만큼의 버튼이 표시
//        startPage: 현재 화면에서 보여주는 페이지 버튼의 시작 번호

//                예를 들어, 사용자가 11번째 페이지에 있으면, 페이지 버튼은 11부터 시작, startPage는 그 페이지 그룹의 첫 번째 버튼 번호
//
//        endPage: 현재 화면에서 보여주는 페이지 버튼의 끝 번호
//
//                예를 들어, 한 번에 10개의 페이지 버튼을 보여줄 경우, endPage는 startPage + buttonAmount - 1로 계산되어, 마지막 페이지 번호가 됨

        int maxPage;			//전체 페이지에서 가장 마지막 페이지
        int startPage;			//한번에 표시될 페이지 버튼의 시작할 페이지
        int endPage;			//한번에 표시될 페이지 버튼의 끝나는 페이지
        int startRow;
        int endRow;

        maxPage = (int) Math.ceil((double) totalCount / limit);

        startPage = (int) (Math.ceil((double) pageNo / buttonAmount) - 1) * buttonAmount + 1;

        endPage = startPage + buttonAmount - 1;

        if(maxPage < endPage){
            endPage = maxPage;
        }

        if(maxPage == 0 && endPage == 0) {
            maxPage = startPage;
            endPage = startPage;
        }


        // MySQL에서는 LIMIT 구문에서 첫 번째 레코드를 0부터 시작하므로, startRow 계산을 0 기반으로 변경
        startRow = (pageNo - 1) * limit;
        endRow = startRow + limit - 1;

        System.out.println("startRow : " + startRow);
        System.out.println("endRow : " + endRow);

        SelectCriteria selectCriteria = new SelectCriteria(pageNo, totalCount, limit, buttonAmount ,maxPage, startPage, endPage, startRow, endRow);

        return selectCriteria;
    }
}
