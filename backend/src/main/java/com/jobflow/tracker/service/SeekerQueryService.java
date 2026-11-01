package com.jobflow.tracker.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.JobApplication;
import com.jobflow.tracker.entity.Role;
import com.jobflow.tracker.repository.AppUserRepository;
import com.jobflow.tracker.repository.JobApplicationRepository;
import com.jobflow.tracker.repository.SeekerSummary;

// 担当者が求職者と応募を閲覧する処理。担当者は閲覧のみなので、書き込みのメソッドは置かない。
@Service
public class SeekerQueryService {

	private static final Logger log = LoggerFactory.getLogger(SeekerQueryService.class);

	private final AppUserRepository userRepository;

	private final JobApplicationRepository applicationRepository;

	public SeekerQueryService(AppUserRepository userRepository, JobApplicationRepository applicationRepository) {
		this.userRepository = userRepository;
		this.applicationRepository = applicationRepository;
	}

	public record SeekerApplications(AppUser seeker, List<JobApplication> applications) {
	}

	@Transactional(readOnly = true)
	public List<SeekerSummary> listSeekers() {
		return userRepository.findSeekerSummaries();
	}

	// 存在しない ID と担当者の ID は、どちらも求職者がいないものとして扱う。
	@Transactional(readOnly = true)
	public SeekerApplications getSeekerApplications(Long advisorId, Long seekerId) {
		AppUser seeker = userRepository.findById(seekerId)
				.filter(user -> user.getRole() == Role.SEEKER)
				.orElseThrow(() -> {
					log.info("Seeker not found (user_id={}, seeker_user_id={})", advisorId, seekerId);
					return new ResourceNotFoundException();
				});
		List<JobApplication> applications = applicationRepository
				.findByOwnerIdOrderByAppliedDateDescIdDesc(seeker.getId());
		log.info("Advisor viewed a seeker's applications (user_id={}, seeker_user_id={})", advisorId, seekerId);
		return new SeekerApplications(seeker, applications);
	}
}
