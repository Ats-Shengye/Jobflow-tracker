package com.jobflow.tracker.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.repository.AppUserRepository;
import com.jobflow.tracker.repository.JobApplicationRepository;

// 求職者が自分の応募を扱う処理。どのメソッドも持ち主の user_id を必ず受け取り、持ち主で絞って読む。
// ログには user_id と application_id だけを出す（企業名・メモなどの中身は出さない）。
@Service
public class JobApplicationService {

	private static final Logger log = LoggerFactory.getLogger(JobApplicationService.class);

	private final JobApplicationRepository applicationRepository;

	private final AppUserRepository userRepository;

	public JobApplicationService(JobApplicationRepository applicationRepository, AppUserRepository userRepository) {
		this.applicationRepository = applicationRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<JobApplication> listOwn(Long ownerId) {
		return applicationRepository.findByOwnerIdOrderByAppliedDateDescIdDesc(ownerId);
	}

	@Transactional(readOnly = true)
	public JobApplication getOwn(Long ownerId, Long applicationId) {
		return findOwn(ownerId, applicationId);
	}

	// users を読み直さず参照だけ作る。利用者がいることはログインの時に確かめている。
	@Transactional
	public JobApplication create(Long ownerId, ApplicationContent content) {
		AppUser owner = userRepository.getReferenceById(ownerId);
		JobApplication saved = applicationRepository.save(content.newApplication(owner));
		log.info("Created an application (user_id={}, application_id={})", ownerId, saved.getId());
		return saved;
	}

	// updated_at はコミット時の flush で @PreUpdate が書き換えるので、返した後の値は更新後になっている。
	@Transactional
	public JobApplication update(Long ownerId, Long applicationId, ApplicationContent content) {
		JobApplication application = findOwn(ownerId, applicationId);
		content.applyTo(application);
		log.info("Updated an application (user_id={}, application_id={})", ownerId, applicationId);
		return application;
	}

	@Transactional
	public void delete(Long ownerId, Long applicationId) {
		applicationRepository.delete(findOwn(ownerId, applicationId));
		log.info("Deleted an application (user_id={}, application_id={})", ownerId, applicationId);
	}

	// 他人の応募と存在しない応募を区別しない（応募があること自体を知らせない）。
	private JobApplication findOwn(Long ownerId, Long applicationId) {
		return applicationRepository.findByIdAndOwnerId(applicationId, ownerId).orElseThrow(() -> {
			log.info("Application not found for the user (user_id={}, application_id={})", ownerId, applicationId);
			return new ResourceNotFoundException();
		});
	}
}
