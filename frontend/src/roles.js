export const ROLE_SEEKER = 'SEEKER'
export const ROLE_ADVISOR = 'ADVISOR'

const HOME_PATHS = {
  [ROLE_SEEKER]: '/applications',
  [ROLE_ADVISOR]: '/advisor/seekers',
}

export function isKnownRole(role) {
  return Object.hasOwn(HOME_PATHS, role)
}

export function homePathFor(role) {
  return isKnownRole(role) ? HOME_PATHS[role] : '/login'
}
