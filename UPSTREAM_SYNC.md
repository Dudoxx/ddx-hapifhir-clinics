# Upstream Synchronization Guide

**Keeping your Dudoxx HAPI FHIR fork synchronized with the official HAPI FHIR JPA Server Starter**

---

## 🔗 Repository Structure

This repository is a **customized fork** of the official HAPI FHIR JPA Server Starter with two Git remotes:

```bash
origin    git@github.com:Dudoxx/ddx-hapifhir-clinics.git
upstream  https://github.com/hapifhir/hapi-fhir-jpaserver-starter.git
```

### Remote Purposes

| Remote | Purpose | URL |
|--------|---------|-----|
| **origin** | Your Dudoxx customizations | `git@github.com:Dudoxx/ddx-hapifhir-clinics.git` |
| **upstream** | Official HAPI FHIR source | `https://github.com/hapifhir/hapi-fhir-jpaserver-starter.git` |

---

## 🔄 Syncing with Upstream Updates

### 1. Check Current Remote Configuration

```bash
git remote -v
```

**Expected output:**
```
origin    git@github.com:Dudoxx/ddx-hapifhir-clinics.git (fetch)
origin    git@github.com:Dudoxx/ddx-hapifhir-clinics.git (push)
upstream  https://github.com/hapifhir/hapi-fhir-jpaserver-starter.git (fetch)
upstream  https://github.com/hapifhir/hapi-fhir-jpaserver-starter.git (push)
```

### 2. Fetch Latest Changes from Upstream

```bash
# Fetch all branches from the official HAPI FHIR repository
git fetch upstream

# View what's new
git log HEAD..upstream/master --oneline
```

### 3. Review Changes Before Merging

```bash
# See detailed differences
git diff HEAD..upstream/master

# Check specific files
git diff HEAD..upstream/master -- pom.xml
git diff HEAD..upstream/master -- src/main/java/
```

### 4. Merge Upstream Changes

**Option A: Merge (Recommended for tracking history)**

```bash
# Create a backup branch first
git branch backup-before-upstream-merge

# Merge upstream changes into your main branch
git checkout main
git merge upstream/master
```

**Option B: Rebase (Creates cleaner history)**

```bash
# Create a backup branch first
git branch backup-before-upstream-rebase

# Rebase your changes on top of upstream
git checkout main
git rebase upstream/master
```

### 5. Resolve Merge Conflicts (if any)

If conflicts occur in your customized files:

```bash
# View conflicted files
git status

# Common conflict locations:
# - src/main/java/ca/uhn/fhir/jpa/starter/interceptor/
# - src/main/resources/application.yaml
# - pom.xml

# Edit conflicts manually, then:
git add <resolved-file>
git merge --continue  # or 'git rebase --continue' if rebasing
```

### 6. Test After Merge

```bash
# Rebuild the project
mvn clean install -DskipTests

# Run tests
mvn test

# Start the server and verify functionality
mvn spring-boot:run
```

### 7. Push to Your Repository

```bash
# Push updated main branch
git push origin main

# If you rebased, force push (use with caution)
git push origin main --force-with-lease
```

---

## 📋 Regular Update Workflow

### Monthly Update Checklist

```bash
# 1. Ensure clean working directory
git status

# 2. Commit any pending changes
git add .
git commit -m "Save work before upstream sync"

# 3. Fetch upstream
git fetch upstream

# 4. Review changes
git log HEAD..upstream/master --oneline

# 5. Create backup
git branch backup-$(date +%Y%m%d)

# 6. Merge upstream
git merge upstream/master

# 7. Test thoroughly
mvn clean install
mvn test

# 8. Push to origin
git push origin main
```

---

## 🛡️ Protected Customizations

### Files You've Customized (DO NOT OVERWRITE)

These files contain Dudoxx-specific changes and may conflict during upstream merges:

#### Custom Interceptors (Partitioning & Auth)
- `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/ApiTokenAuthInterceptor.java`
- `src/main/java/ca/uhn/fhir/jpa/starter/interceptor/ClinicPartitionInterceptor.java`

#### Configuration Files
- `src/main/resources/application.yaml` (PostgreSQL, partitions, auth)
- `src/main/resources/init-partitions.sql` (7 clinic partitions)

#### Documentation
- `DUDOXX_CUSTOMIZATIONS.md`
- `AGENTS.md`
- `README.md` (customized with Dudoxx branding)

#### Docker & Build
- `docker-compose.yml` (PostgreSQL configuration)
- `Dockerfile` (if customized)

### During Merge Conflicts

**Always preserve Dudoxx customizations in these files:**

1. **application.yaml** - Keep your PostgreSQL connection, partition settings
2. **Interceptors** - Keep entire `interceptor/` package
3. **init-partitions.sql** - Keep clinic partition definitions

**Accept upstream changes for:**
- Dependency upgrades in `pom.xml` (review carefully)
- Core HAPI FHIR code improvements
- Bug fixes and security patches

---

## 🔍 Tracking Your Customizations

### View All Dudoxx-Specific Changes

```bash
# Show commits you've added since forking
git log upstream/master..HEAD

# Show file differences
git diff upstream/master...HEAD

# List modified files
git diff --name-only upstream/master...HEAD
```

### Your Key Commits

```
aac759a - Add partitions for ddx-default-clinic and ddx-shared-clinic
43ba7d0 - Add Dudoxx multi-tenant clinic enhancements
```

### Tagging Your Versions

```bash
# Tag before major upstream merges
git tag -a v1.0-dudoxx -m "Dudoxx v1.0 - Pre upstream merge"
git push origin v1.0-dudoxx
```

---

## 🚨 Emergency Rollback

If an upstream merge breaks functionality:

### Rollback Using Backup Branch

```bash
# List backup branches
git branch --list 'backup-*'

# Restore from backup
git reset --hard backup-2024-11-15
git push origin main --force-with-lease
```

### Rollback to Specific Commit

```bash
# Find your last working commit
git log --oneline -10

# Reset to that commit
git reset --hard <commit-hash>
git push origin main --force-with-lease
```

---

## 📊 Monitoring Upstream Activity

### Subscribe to HAPI FHIR Releases

1. Visit: https://github.com/hapifhir/hapi-fhir-jpaserver-starter/releases
2. Click "Watch" → "Custom" → "Releases"
3. Review release notes before syncing

### Check for Security Updates

```bash
# Review upstream changes in security-sensitive areas
git fetch upstream
git log HEAD..upstream/master -- pom.xml
git log HEAD..upstream/master -- src/main/java/.../security/
```

---

## 🔧 Advanced Operations

### Cherry-Pick Specific Upstream Commits

If you only want specific fixes without a full merge:

```bash
# Fetch upstream
git fetch upstream

# View commits
git log upstream/master --oneline

# Cherry-pick specific commit
git cherry-pick <commit-hash>

# Push
git push origin main
```

### Update Dependency Versions Only

```bash
# Fetch upstream pom.xml changes
git fetch upstream
git checkout upstream/master -- pom.xml

# Review changes
git diff HEAD pom.xml

# Commit if acceptable
git commit -m "Update dependencies from upstream"
```

---

## 📝 Best Practices

### ✅ DO

- ✅ Fetch upstream monthly to stay current
- ✅ Create backup branches before major merges
- ✅ Test thoroughly after every upstream merge
- ✅ Document any custom merge conflict resolutions
- ✅ Review upstream release notes before syncing
- ✅ Tag your versions before risky operations

### ❌ DON'T

- ❌ Force push without `--force-with-lease`
- ❌ Merge upstream without testing
- ❌ Overwrite custom interceptors with upstream defaults
- ❌ Skip backup branches before major operations
- ❌ Ignore merge conflicts (resolve properly)

---

## 🆘 Troubleshooting

### Problem: "fatal: refusing to merge unrelated histories"

```bash
git merge upstream/master --allow-unrelated-histories
```

### Problem: Lost customizations after merge

```bash
# Restore from backup branch
git checkout backup-<date> -- src/main/java/ca/uhn/fhir/jpa/starter/interceptor/
git commit -m "Restore Dudoxx interceptors"
```

### Problem: Merge conflicts in pom.xml

```bash
# Accept their version, then re-apply your changes manually
git checkout --theirs pom.xml
# Edit pom.xml to add back any Dudoxx-specific dependencies
git add pom.xml
git merge --continue
```

### Problem: Can't find upstream remote

```bash
# Re-add upstream remote
git remote add upstream https://github.com/hapifhir/hapi-fhir-jpaserver-starter.git
git fetch upstream
```

---

## 📞 Support

- **Upstream Issues**: https://github.com/hapifhir/hapi-fhir-jpaserver-starter/issues
- **HAPI FHIR Docs**: https://hapifhir.io/
- **Dudoxx Customizations**: See `DUDOXX_CUSTOMIZATIONS.md`

---

**Last Updated:** November 2024  
**Maintained by:** Dudoxx Engineering Team
