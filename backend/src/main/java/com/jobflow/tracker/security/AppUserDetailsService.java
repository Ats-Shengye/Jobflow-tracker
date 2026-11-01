package com.jobflow.tracker.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.jobflow.tracker.repository.AppUserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

	private final AppUserRepository userRepository;

	public AppUserDetailsService(AppUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	// 例外のメッセージにログイン ID を入れない（ログに残るため）。利用者への応答では ID 違いとパスワード違いを区別しない。
	@Override
	public UserDetails loadUserByUsername(String loginId) {
		return userRepository.findByLoginId(loginId)
				.map(LoginUser::from)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
