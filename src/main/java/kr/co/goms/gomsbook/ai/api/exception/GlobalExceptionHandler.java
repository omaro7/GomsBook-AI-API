package kr.co.goms.gomsbook.ai.api.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import kr.co.goms.gomsbook.ai.conversation.exception.ConversationProjectMismatchException;

@RestControllerAdvice
public final class GlobalExceptionHandler {

	@ExceptionHandler(ConversationProjectMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleConversationProjectMismatch(
			ConversationProjectMismatchException exception) {

		Map<String, Object> body = new LinkedHashMap<>();

		body.put("error", "CONVERSATION_PROJECT_MISMATCH");
		body.put("message", exception.getMessage());

		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(body);
	}
}