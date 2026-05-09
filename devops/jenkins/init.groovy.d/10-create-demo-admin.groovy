import jenkins.model.Jenkins
import hudson.security.HudsonPrivateSecurityRealm
import hudson.security.GlobalMatrixAuthorizationStrategy

def instance = Jenkins.get()
def realm = instance.getSecurityRealm()

if (realm instanceof HudsonPrivateSecurityRealm) {
    def username = "jury-admin"
    def password = "NephroDevops2026!"

    if (realm.getUser(username) == null) {
        println("Creating fallback Jenkins demo admin: ${username}")
        realm.createAccount(username, password)
    } else {
        println("Fallback Jenkins demo admin already exists: ${username}")
    }

    def strategy = instance.getAuthorizationStrategy()
    if (strategy instanceof GlobalMatrixAuthorizationStrategy) {
        strategy.add(Jenkins.ADMINISTER, username)
    }

    instance.save()
}
