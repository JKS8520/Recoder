package yuhan.hgcq.client.controller;

import android.content.Context;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import yuhan.hgcq.client.config.NetworkClient;
import yuhan.hgcq.client.model.dto.member.LoginForm;
import yuhan.hgcq.client.model.dto.member.SignupForm;
import yuhan.hgcq.client.model.dto.member.MemberUpdateForm;
import yuhan.hgcq.client.model.service.MemberService;

public class MemberController {

    private MemberService memberService;

    public MemberController(Context context) {
        NetworkClient client = NetworkClient.getInstance(context.getApplicationContext());
        memberService = client.getMemberService();
    }

    public void joinMember(SignupForm signupForm, Callback<ResponseBody> callback) {
        Call<ResponseBody> call = memberService.joinMember(signupForm);
        call.enqueue(callback);
    }

    public void duplicateName(String name, Callback<Boolean> callback) {
        Call<Boolean> call = memberService.duplicateName(name);
        call.enqueue(callback);
    }

    public void duplicateEmail(String email, Callback<Boolean> callback) {
        Call<Boolean> call = memberService.duplicateEmail(email);
        call.enqueue(callback);
    }

    public void loginMember(LoginForm loginForm, Callback<ResponseBody> callback) {
        Call<ResponseBody> call = memberService.loginMember(loginForm);
        call.enqueue(callback);
    }

    public void logoutMember(Callback<ResponseBody> callback) {
        Call<ResponseBody> call = memberService.logoutMember();
        call.enqueue(callback);
    }

    public void updateMember(MemberUpdateForm updateForm, Callback<ResponseBody> callback) {
        Call<ResponseBody> call = memberService.updateMember(updateForm);
        call.enqueue(callback);
    }

}