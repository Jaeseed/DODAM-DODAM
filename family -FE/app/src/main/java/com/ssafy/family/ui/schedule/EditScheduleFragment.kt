package com.ssafy.family.ui.schedule

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.ssafy.family.R
import com.ssafy.family.data.remote.req.ScheduleReq
import com.ssafy.family.databinding.CalendarHeaderBinding
import com.ssafy.family.databinding.CalendarSelectingDayBinding
import com.ssafy.family.databinding.FragmentEditScheduleBinding
import com.ssafy.family.ui.home.LoginViewModel
import com.ssafy.family.util.CalendarUtil
import com.ssafy.family.util.CalendarUtil.getDrawableCompat
import com.ssafy.family.util.CalendarUtil.makeInVisible
import com.ssafy.family.util.CalendarUtil.makeVisible
import com.ssafy.family.util.CalendarUtil.setTextColorRes
import com.ssafy.family.util.CalendarUtil.stringToLocalDate
import com.ssafy.family.util.LoginUtil
import com.ssafy.family.util.Status
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val ScheduleId = "sId"

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.O)
class EditScheduleFragment : Fragment() {

    private lateinit var binding: FragmentEditScheduleBinding
    private val editScheduleViewModel by activityViewModels<EditScheduleViewModel>()
    private val loginViewModel by activityViewModels<LoginViewModel>()

    private var scheduleId: Long? = null
    private var selectedDate: LocalDate? = null
    private val today = LocalDate.now()
    private val headerDateFormatter = DateTimeFormatter.ofPattern("MMM d'???'")
    private var startDate: LocalDate? = null
    private var endDate: LocalDate? = null

    private val startBackground: GradientDrawable by lazy {
        requireContext().getDrawableCompat(R.drawable.box_calendar_start) as GradientDrawable
    }
    private val endBackground: GradientDrawable by lazy {
        requireContext().getDrawableCompat(R.drawable.box_calendar_end) as GradientDrawable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            scheduleId = it.getLong(ScheduleId)
            if(scheduleId == null){
                Toast.makeText(requireActivity(), "???????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentEditScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        //?????? ?????? ??????
        editScheduleViewModel.getOneSchedule(scheduleId!!)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //?????? ??? parent ????????? ?????????
        (activity as ScheduleActivity).apply {
            changeHeader("?????? ??????", "??????", "??????")
            binding.scheduleButtonInclude.button.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.schedule_frame, DetailScheduleFragment.newInstance(scheduleId!!))
                    .commit()
            }
            binding.scheduleTopInclude.backbtn.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.schedule_frame, DetailScheduleFragment.newInstance(scheduleId!!))
                    .commit()
            }
        }

        //?????? ?????? ?????? ??????
        (activity as ScheduleActivity).binding.scheduleButtonInclude.button2.setOnClickListener {
            if(binding.scheduleTitle.text.length < 2 || binding.scheduleTitle.text.length > 20){
                Toast.makeText(requireContext(), "?????? ????????? 2~20?????? ??????????????????", Toast.LENGTH_SHORT).show()
            } else if(binding.scheduleContent.text!!.length < 2){
                Toast.makeText(requireContext(), "?????? ????????? 2??? ?????? ??????????????????", Toast.LENGTH_SHORT).show()
            }else if(startDate == null && endDate == null){
                Toast.makeText(requireContext(), "?????? ????????? ??????????????????", Toast.LENGTH_SHORT).show()
            }else{
                editScheduleViewModel.editSchedule(
                    scheduleId!!,
                    ScheduleReq(
                        binding.scheduleTitle.text.toString(),
                        binding.scheduleContent.text.toString(),
                        CalendarUtil.dayLocalDateToString(startDate!!),
                        CalendarUtil.dayLocalDateToString(endDate!!)
                    )
                )
            }
        }

        //?????? ?????? ?????? ?????? ?????????
        editScheduleViewModel.editRequestLiveData.observe(requireActivity()) {
            when (it.status) {
                Status.SUCCESS -> {
                    Toast.makeText(requireContext(), "?????? ????????? ??????????????????.", Toast.LENGTH_SHORT).show()
                    dismissLoading()
                    requireActivity().finish()
                }
                Status.ERROR -> {
                    Toast.makeText(requireActivity(), "???????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
                    dismissLoading()
                    requireActivity().finish()
                }
                Status.LOADING -> {
                    setLoading()
                }
                Status.EXPIRED -> {
                    dismissLoading()
                    loginViewModel.MakeRefresh(LoginUtil.getUserInfo()!!.refreshToken)
                    Toast.makeText(requireActivity(), "?????? ??????????????????", Toast.LENGTH_SHORT).show()
                }
            }
        }

        //?????? ?????? ?????? ?????? ?????????
        editScheduleViewModel.getOneRequestLiveData.observe(requireActivity()){
            when (it.status) {
                Status.SUCCESS -> {
                    if(it.data!!.schedule != null){
                        binding.startdate.text = headerDateFormatter.format(stringToLocalDate(it.data.schedule!!.startDate))
                        binding.enddate.text = headerDateFormatter.format(stringToLocalDate(it.data.schedule.endDate))
                        binding.scheduleTitle.setText(it.data.schedule.title)
                        binding.scheduleContent.setText(it.data.schedule.content)
                        startDate = stringToLocalDate(it.data.schedule.startDate)
                        endDate = stringToLocalDate(it.data.schedule.endDate)
                    }
                    dismissLoading()
                    initCalendar()
                }
                Status.ERROR -> {
                    Toast.makeText(requireActivity(), "???????????? ?????? ???????????????.", Toast.LENGTH_SHORT).show()
                    dismissLoading()
                    requireActivity().finish()
                }
                Status.LOADING -> {
                    setLoading()
                }
                Status.EXPIRED -> {
                    dismissLoading()
                    loginViewModel.MakeRefresh(LoginUtil.getUserInfo()!!.refreshToken)
                    Toast.makeText(requireActivity(), "?????? ??????????????????", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initCalendar() {

        //?????? ?????? ??????
        val daysOfWeek = CalendarUtil.daysOfWeekFromLocale()
        var currentMonth = YearMonth.now()
        binding.calendar.apply {
            setup(currentMonth.minusMonths(100), currentMonth.plusMonths(100), daysOfWeek.first())
            scrollToMonth(currentMonth)
        }

        //?????? ??????(??????, ??????, ?????? ?????? ???)
        binding.calendar.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val textView = container.binding.dayText
                val roundBgView = container.binding.roundBackground

                textView.text = null
                textView.background = null
                roundBgView.makeInVisible()

                val startDate = startDate
                val endDate = endDate

                when (day.owner) {
                    DayOwner.THIS_MONTH -> {
                        textView.text = day.day.toString()

                        when {
                            day.date == startDate && startDate != endDate -> {
                                textView.setTextColorRes(R.color.white)
                                textView.background = startBackground
                            }
                            startDate == day.date  -> {
                                textView.setTextColorRes(R.color.white)
                                roundBgView.makeVisible()
                                roundBgView.setBackgroundResource(R.drawable.box_calendar_single)
                            }
                            startDate != null && endDate != null && (day.date > startDate && day.date < endDate) -> {
                                textView.setTextColorRes(R.color.white)
                                textView.setBackgroundResource(R.drawable.box_calendar_middle)
                            }
                            day.date == endDate -> {
                                textView.setTextColorRes(R.color.white)
                                textView.background = endBackground
                            }
                            day.date == today -> {
                                roundBgView.makeVisible()
                                roundBgView.setBackgroundResource(R.drawable.circle_today)
                                textView.setTextColorRes(R.color.black)
                            }
                            else -> textView.setTextColorRes(R.color.black)
                        }
                    }

                    // ????????? ????????? ?????? ???????????? ??????, ????????? ?????? ?????? ?????? ??????
                    DayOwner.PREVIOUS_MONTH ->
                        if (startDate != null && endDate != null && CalendarUtil.isInDateBetween(day.date, startDate, endDate)) {
                            textView.setBackgroundResource(R.drawable.box_calendar_middle)
                        }
                    DayOwner.NEXT_MONTH ->
                        if (startDate != null && endDate != null && CalendarUtil.isOutDateBetween(day.date, startDate, endDate)) {
                            textView.setBackgroundResource(R.drawable.box_calendar_middle)
                        }
                }
            }
        }

        //?????? ????????? ?????????
        binding.calendar.monthScrollListener = {
            selectDate(it.yearMonth.atDay(1))
        }

        //?????? ?????? ??????
        class MonthViewContainer(view: View) : ViewContainer(view) {
            val legendLayout = CalendarHeaderBinding.bind(view).legendLayout
        }
        binding.calendar.monthHeaderBinder = object :
            MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {}
        }
    }

    //?????? ???????????? ?????????????????? ???????????? ?????????
    inner class DayViewContainer(view: View) : ViewContainer(view) {
        lateinit var day: CalendarDay // Will be set when this container is bound.
        val binding = CalendarSelectingDayBinding.bind(view)

        init {
            view.setOnClickListener {
                if (day.owner == DayOwner.THIS_MONTH) {
                    val date = day.date
                    if (startDate != null) {
                        if (date < startDate || endDate != startDate) {
                            startDate = date
                            endDate = date
                        } else if (date != startDate) {
                            endDate = date
                        }
                    } else {
                        startDate = date
                        endDate = date
                    }
                    this@EditScheduleFragment.binding.calendar.notifyCalendarChanged()
                    bindSummaryViews()
                }
            }
        }
    }

    //????????? ?????? : ?????? ????????? ??????????????? ????????????.
    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { binding.calendar.notifyDateChanged(it) }
            binding.calendar.notifyDateChanged(date)
        }
    }

    // ????????? ????????? ???????????? ????????????.
    private fun bindSummaryViews() {
        binding.startdate.apply {
            text = headerDateFormatter.format(startDate)
        }

        binding.enddate.apply {
            text = headerDateFormatter.format(endDate)
        }
    }

    //?????? ?????????
    private fun setLoading() {
        binding.progressBarAddSLoading.visibility = View.VISIBLE
    }

    private fun dismissLoading() {
        binding.progressBarAddSLoading.visibility = View.GONE
    }

    companion object {
        @JvmStatic
        fun newInstance(scheduleId: Long) =
            EditScheduleFragment().apply {
                arguments = Bundle().apply {
                    putLong(ScheduleId, scheduleId)
                }
            }
    }

}