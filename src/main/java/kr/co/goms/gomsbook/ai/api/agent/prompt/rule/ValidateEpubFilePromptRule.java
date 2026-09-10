package kr.co.goms.gomsbook.ai.api.agent.prompt.rule;

public final class ValidateEpubFilePromptRule implements AgentPromptRule {

	private static final String TOOL_NAME = "validate_epub_file";
    private static final String PROMPT = """
            [EPUB 파일 검증 결과 응답 규칙]

            validate_epub_file Tool 실행 결과를 응답할 때 다음 규칙을 적용합니다.

            1. 전체 EPUB 검증 상태를 먼저 요약합니다.
            2. Fatal, Error, Warning의 주요 원인을 한국어로 설명합니다.
            3. 설명 아래에 data.issues의 모든 검증 이슈를 Markdown 테이블로 출력합니다.

            테이블 형식:

            | 구분 | 코드 | 파일 | 행 | 열 | 오류 내용 |
            |---|---|---|---:|---:|---|

            출력 규칙:
            - data.issues의 모든 항목을 출력합니다.
            - 검증 이슈를 병합하거나 중복 제거하지 않습니다.
            - 동일한 오류 코드라도 파일, 행 또는 열이 다르면 각각 별도 행으로 출력합니다.
            - 파일은 epubPath를 사용합니다.
            - 행은 line을 사용합니다.
            - 열은 column을 사용합니다.
            - 오류 내용은 message를 사용합니다.
            - 값이 없는 경우 "-"를 출력합니다.
            - Tool이 반환한 검증 이슈 순서를 유지합니다.
            """;

    @Override
    public String getToolName() {
        return TOOL_NAME;
    }

    @Override
    public String getPrompt() {
        return PROMPT;
    }
}