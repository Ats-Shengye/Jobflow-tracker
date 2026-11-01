package com.jobflow.tracker.controller;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jobflow.tracker.dto.MessageResponse;
import com.jobflow.tracker.service.DuplicateLoginIdException;

// 応答の本文を {"message": "…"} にそろえる。想定外の例外は中身を出さずに 500 にする。
@RestControllerAdvice
public class ApiExceptionHandler {

	static final String INVALID_INPUT_MESSAGE = "入力内容を確認してください";

	static final String MALFORMED_REQUEST_MESSAGE = "リクエストの形式が正しくありません";

	static final String LOGIN_FAILED_MESSAGE = "ログインIDまたはパスワードが正しくありません";

	static final String DUPLICATE_LOGIN_ID_MESSAGE = "このログインIDは使用できません";

	static final String SERVER_ERROR_MESSAGE = "サーバーでエラーが発生しました。時間をおいてもう一度お試しください";

	private static final String APPLICATION_PACKAGE_PREFIX = "com.jobflow.tracker.";

	// cause が循環していても止まるようにする上限。
	private static final int MAX_CAUSE_DEPTH = 20;

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<MessageResponse> handleValidation(MethodArgumentNotValidException exception) {
		Map<String, String> errors = new TreeMap<>();
		Map<String, Integer> chosenPriority = new TreeMap<>();
		// 入力した値（パスワードなど）が入るので、FieldError の rejectedValue は返さない・ログに出さない。
		for (FieldError error : exception.getBindingResult().getFieldErrors()) {
			String key = toSnakeCase(error.getField());
			int priority = priorityOf(error.getCode());
			if (!chosenPriority.containsKey(key) || priority < chosenPriority.get(key)) {
				chosenPriority.put(key, priority);
				errors.put(key, error.getDefaultMessage());
			}
		}
		return ResponseEntity.badRequest().body(new MessageResponse(INVALID_INPUT_MESSAGE, errors));
	}

	// JSON が壊れている、項目の型が違う（文字列の所にオブジェクトなど）、本文が無い。
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<MessageResponse> handleUnreadable(HttpMessageNotReadableException exception) {
		return ResponseEntity.badRequest().body(MessageResponse.of(MALFORMED_REQUEST_MESSAGE));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException exception) {
		log.info("Login failed");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MessageResponse.of(LOGIN_FAILED_MESSAGE));
	}

	@ExceptionHandler(DuplicateLoginIdException.class)
	public ResponseEntity<MessageResponse> handleDuplicateLoginId(DuplicateLoginIdException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(MessageResponse.of(DUPLICATE_LOGIN_ID_MESSAGE));
	}

	// コントローラーの中で権限の確認に引っかかった場合。下の Exception で受けると 500 になってしまうため分ける。
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<MessageResponse> handleAccessDenied(AccessDeniedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(MessageResponse.of(messageFor(HttpStatus.FORBIDDEN)));
	}

	// Spring MVC の例外（404・405・415 など）は ErrorResponse として状態コードを持っているので、それを使う。
	// それ以外は想定外として 500 にし、例外のメッセージは応答に入れない（SQL や内部の情報が入りうる）。
	@ExceptionHandler(Exception.class)
	public ResponseEntity<MessageResponse> handleOther(Exception exception) {
		if (exception instanceof ErrorResponse errorResponse && !errorResponse.getStatusCode().is5xxServerError()) {
			HttpStatusCode status = errorResponse.getStatusCode();
			log.debug("Request rejected with status {}", status.value());
			return ResponseEntity.status(status).body(MessageResponse.of(messageFor(status)));
		}
		// 例外のメッセージには DB の値（メモなど）が入りうるので、ERROR には種類と発生箇所だけを書く。
		// 中身とスタックトレースは DEBUG（既定では出ない）。
		log.error("Unhandled exception: {} at {}", exception.getClass().getName(), firstApplicationFrame(exception));
		log.debug("Unhandled exception detail", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(MessageResponse.of(SERVER_ERROR_MESSAGE));
	}

	// cause を辿り、このアプリのコードで最初に現れる呼び出し位置を返す。Spring や Hibernate の中だけで起きた時は unknown。
	static String firstApplicationFrame(Throwable exception) {
		int depth = 0;
		for (Throwable current = exception; current != null && depth < MAX_CAUSE_DEPTH; current = current.getCause()) {
			for (StackTraceElement frame : current.getStackTrace()) {
				if (frame.getClassName().startsWith(APPLICATION_PACKAGE_PREFIX)) {
					return frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber();
				}
			}
			depth++;
		}
		return "unknown";
	}

	private static String messageFor(HttpStatusCode status) {
		return switch (status.value()) {
		case 401 -> "ログインしてください";
		case 403 -> "この操作を行う権限がありません";
		case 404 -> "対象が見つかりません";
		default -> MALFORMED_REQUEST_MESSAGE;
		};
	}

	// 1つの項目で複数のチェックに引っかかった時、「未入力」→「文字数」→「文字種」の順に1つだけ返す。
	private static int priorityOf(String constraintCode) {
		if (constraintCode == null) {
			return 3;
		}
		return switch (constraintCode) {
		case "NotBlank", "NotEmpty", "NotNull" -> 0;
		case "Size" -> 1;
		case "Pattern" -> 2;
		default -> 3;
		};
	}

	// DTO の項目名（loginId）を、JSON のキー（login_id）にそろえる。
	static String toSnakeCase(String fieldName) {
		return fieldName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
	}
}
