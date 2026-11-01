package com.jobflow.tracker.service;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.repository.AppUserRepository;

@Service
public class UserRegistrationService {

	private static final Logger log = LoggerFactory.getLogger(UserRegistrationService.class);

	private final AppUserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	public UserRegistrationService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	// 自分で登録できるのは求職者だけなので、role は引数に取らない。
	@Transactional
	public AppUser registerSeeker(String loginId, String rawPassword, String name) {
		if (userRepository.existsByLoginId(loginId)) {
			log.info("Registration rejected: login_id already in use");
			throw new DuplicateLoginIdException();
		}
		AppUser user = new AppUser(loginId, passwordEncoder.encode(rawPassword), name, Role.SEEKER);
		AppUser saved;
		try {
			// 同時に同じ ID で登録されると上の確認をすり抜けるので、flush して UNIQUE 制約の違反をここで受け取る。
			saved = userRepository.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			if (!isUniqueViolation(e)) {
				throw e;
			}
			// 例外の中身は書かない。H2 のメッセージには重複したログイン ID がそのまま入る。
			log.info("Registration rejected: login_id already in use (unique constraint)");
			throw new DuplicateLoginIdException();
		}
		log.info("Registered a new seeker (user_id={})", saved.getId());
		return saved;
	}

	// users にある UNIQUE 制約はログイン ID だけなので、UNIQUE の違反ならログイン ID の重複と判断できる。
	// Spring はこの違反を DuplicateKeyException に分けず DataIntegrityViolationException にまとめるため、Hibernate の例外を見る。
	static boolean isUniqueViolation(DataIntegrityViolationException exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation.getKind() == ConstraintKind.UNIQUE;
			}
		}
		return false;
	}
}
