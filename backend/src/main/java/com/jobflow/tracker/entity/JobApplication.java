package com.jobflow.tracker.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

// Spring の ApplicationRunner などと紛れないよう JobApplication にした。
// 文字数などの入力チェックは API の DTO で行い、ここでは status と面接回数の関係だけを守る。
@Entity
@Table(name = "applications")
public class JobApplication {

	public static final int MIN_INTERVIEW_ROUND = 1; // 一次面接

	public static final int MAX_INTERVIEW_ROUND = 5;

	public static final int TITLE_MAX_LENGTH = 100;

	public static final int FREE_TEXT_MAX_LENGTH = 100;

	public static final int MEMO_MAX_LENGTH = 1000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "application_id")
	private Long id;

	// 一覧を取る時に応募ごとに users を読みに行かないよう LAZY にする。
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser owner;

	@Column(name = "company_name", nullable = false, length = TITLE_MAX_LENGTH)
	private String companyName;

	@Column(name = "position", nullable = false, length = TITLE_MAX_LENGTH)
	private String position;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_insurance", nullable = false, length = 20)
	private EmploymentInsurance employmentInsurance;

	@Column(name = "applied_date", nullable = false)
	private LocalDate appliedDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ApplicationStatus status;

	@Column(name = "interview_round")
	private Integer interviewRound;

	@Column(name = "salary", length = FREE_TEXT_MAX_LENGTH)
	private String salary;

	@Column(name = "work_location", length = FREE_TEXT_MAX_LENGTH)
	private String workLocation;

	@Column(name = "memo", length = MEMO_MAX_LENGTH)
	private String memo;

	// Instant は UTC で保存されるので、サーバーのタイムゾーンに左右されない。+09:00 への変換は API 側で行う。
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected JobApplication() {
	}

	public JobApplication(AppUser owner, String companyName, String position,
			EmploymentInsurance employmentInsurance, LocalDate appliedDate, ApplicationStatus status,
			Integer interviewRound, String salary, String workLocation, String memo) {
		this.owner = Objects.requireNonNull(owner, "owner must not be null");
		applyContent(companyName, position, employmentInsurance, appliedDate, status, interviewRound, salary,
				workLocation, memo);
	}

	// PUT で全項目を置き換える。持ち主は変えない。
	public void update(String companyName, String position, EmploymentInsurance employmentInsurance,
			LocalDate appliedDate, ApplicationStatus status, Integer interviewRound, String salary,
			String workLocation, String memo) {
		applyContent(companyName, position, employmentInsurance, appliedDate, status, interviewRound, salary,
				workLocation, memo);
	}

	private void applyContent(String companyName, String position, EmploymentInsurance employmentInsurance,
			LocalDate appliedDate, ApplicationStatus status, Integer interviewRound, String salary,
			String workLocation, String memo) {
		this.companyName = Objects.requireNonNull(companyName, "companyName must not be null");
		this.position = Objects.requireNonNull(position, "position must not be null");
		this.employmentInsurance = Objects.requireNonNull(employmentInsurance,
				"employmentInsurance must not be null");
		this.appliedDate = Objects.requireNonNull(appliedDate, "appliedDate must not be null");
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.interviewRound = resolveInterviewRound(status, interviewRound);
		this.salary = salary;
		this.workLocation = workLocation;
		this.memo = memo;
	}

	// 面接回数は面接の時だけ持つ。他のステータスに変えたら空にし、面接に戻した時は入れ直してもらう。
	private static Integer resolveInterviewRound(ApplicationStatus status, Integer interviewRound) {
		if (status != ApplicationStatus.INTERVIEW) {
			return null;
		}
		if (interviewRound == null || interviewRound < MIN_INTERVIEW_ROUND || interviewRound > MAX_INTERVIEW_ROUND) {
			throw new IllegalArgumentException("interviewRound must be between " + MIN_INTERVIEW_ROUND + " and "
					+ MAX_INTERVIEW_ROUND + " when status is INTERVIEW");
		}
		return interviewRound;
	}

	// DB の精度（マイクロ秒）に合わせて切り捨て、保存前と読み直した後の値をそろえる。
	// 値が何も変わらない更新では Hibernate がこれを呼ばないので、updated_at もそのまま。
	@PrePersist
	@PreUpdate
	void refreshUpdatedAt() {
		this.updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	public Long getId() {
		return id;
	}

	public AppUser getOwner() {
		return owner;
	}

	public String getCompanyName() {
		return companyName;
	}

	public String getPosition() {
		return position;
	}

	public EmploymentInsurance getEmploymentInsurance() {
		return employmentInsurance;
	}

	public LocalDate getAppliedDate() {
		return appliedDate;
	}

	public ApplicationStatus getStatus() {
		return status;
	}

	public Integer getInterviewRound() {
		return interviewRound;
	}

	public String getSalary() {
		return salary;
	}

	public String getWorkLocation() {
		return workLocation;
	}

	public String getMemo() {
		return memo;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	// メモは私的な内容が入りうるのでログに出さない。owner は遅延読み込みなので触らない。
	@Override
	public String toString() {
		return "JobApplication{id=" + id + ", status=" + status + "}";
	}
}
